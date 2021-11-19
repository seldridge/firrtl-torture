/** Copyright 2021 SiFive
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

using scala 2.13.6
using lib edu.berkeley.cs::chisel3::3.5.0-RC1

import chisel3._
import chisel3.stage.ChiselStage

object Generators {

  /** This turns off _most_ source locators. */
  implicit val noSourceInfo = chisel3.internal.sourceinfo.UnlocatableSourceInfo

  /** A module that contains a parametric binary operation of the form (A, A) => B. */
  class BinaryOp[A <: Data, B <: Data](override val desiredName: String, gen_in: => A, gen_out: => B, binOp: (A, A) => B) extends Module {
    val in = Seq.fill(2)(IO(Input(gen_in)))
    val out = Seq.fill(4)(IO(Output(gen_out)))
    val invalid = Wire(gen_in)
    invalid := DontCare
    val r = Seq.fill(4)(Reg(gen_out))
    r.zipWithIndex.zip(out).foreach { case ((r, i), out) =>
      r := binOp(
        if (i % 4 >> 1 == 0) in(1) else invalid,
        if (i % 2 == 0) in(0) else invalid
      )
      out := r
    }
  }

  /** A module that contains a parametric unary operation of the form (A) => B. */
  class UnaryOp[A <: Data, B <: Data](override val desiredName: String, gen_in: => A, gen_out: => B, binOp: (A) => B) extends Module {
    val in = IO(Input(gen_in))
    val out = Seq.fill(2)(IO(Output(gen_out)))
    val invalid = Wire(gen_in)
    invalid := DontCare
    val r = Seq.fill(2)(Reg(gen_out))
    r.zipWithIndex.zip(out).foreach { case ((r, i), out) =>
      r := binOp(
        if (i % 2 == 0) in else invalid
      )
      out := r
    }
  }
}

/** An object that contains descriptions of all FIRRTL operations. */
object FIRRTLOps {

  /** Binary operations of type (UInt, UInt) => UInt */
  val binaryOpsUUU = Seq(
    ("and", UInt(1.W), UInt(1.W), (a: UInt, b: UInt) => a & b),
    ("or", UInt(1.W), UInt(1.W), (a: UInt, b: UInt) => a | b),
    ("xor", UInt(1.W), UInt(1.W), (a: UInt, b: UInt) => a ^ b),
    ("add", UInt(4.W), UInt(5.W), (a: UInt, b: UInt) => a + b),
    ("sub", UInt(4.W), UInt(5.W), (a: UInt, b: UInt) => a - b),
    ("mul", UInt(4.W), UInt(8.W), (a: UInt, b: UInt) => a * b),
    ("div", UInt(4.W), UInt(4.W), (a: UInt, b: UInt) => a / b),
    ("rem", UInt(4.W), UInt(4.W), (a: UInt, b: UInt) => a % b),
    ("cat", UInt(2.W), UInt(4.W), (a: UInt, b: UInt) => a ## b),
    ("dshl", UInt(2.W), UInt(5.W), (a: UInt, b: UInt) => a << b),
    ("dshr", UInt(2.W), UInt(2.W), (a: UInt, b: UInt) => a > b),
  )

  /** Binary operations of type (UInt, UInt) => Bool */
  val binaryOpsUUB = Seq(
    ("lt", UInt(4.W), Bool(), (a: UInt, b: UInt) => a < b),
    ("leq", UInt(4.W), Bool(), (a: UInt, b: UInt) => a <= b),
    ("gt", UInt(4.W), Bool(), (a: UInt, b: UInt) => a > b),
    ("geq", UInt(4.W), Bool(), (a: UInt, b: UInt) => a >= b),
    ("eq", UInt(4.W), Bool(), (a: UInt, b: UInt) => a === b),
    ("neq", UInt(4.W), Bool(), (a: UInt, b: UInt) => a =/= b),
  )

  /** Unary operations of type (UInt) => UInt */
  val unaryOpsUU = Seq(
    ("pad", UInt(1.W), UInt(2.W), (a: UInt) => a.pad(2)),
    ("shl", UInt(2.W), UInt(4.W), (a: UInt) => a << 2),
    ("shr", UInt(4.W), UInt(2.W), (a: UInt) => a >> 2),
    ("neg", UInt(4.W), UInt(5.W), (a: UInt) => -a),
    ("not", UInt(4.W), UInt(4.W), (a: UInt) => ~a),
    ("bits", UInt(4.W), UInt(2.W), (a: UInt) => a(3, 2)),
    ("head", UInt(4.W), UInt(2.W), (a: UInt) => a.head(2)),
    ("tail", UInt(4.W), UInt(2.W), (a: UInt) => a.tail(2)),
    ("tail", UInt(4.W), UInt(2.W), (a: UInt) => a.tail(2)),
  )

  /** Unary operations of type (SInt) => UInt */
  val unaryOpsSU = Seq(
    ("asUInt", SInt(2.W), UInt(2.W), (a: SInt) => a.asUInt()),
  )

  /** Unary operations of type (UInt) => SInt */
  val unaryOpsUS = Seq(
    ("asSInt", UInt(2.W), SInt(2.W), (a: UInt) => a.asSInt()),
    ("cvt", UInt(4.W), SInt(5.W), (a: UInt) => a.zext()),
  )

  /** Unary operations of type (Bool) => Clock */
  val unaryOpsBC = Seq(
    ("asClock", Bool(), Clock(), (a: Bool) => a.asClock()),
  )

  /** Unary operations of type (UInt) => Bool */
  val unaryOpsUB = Seq(
    ("andr", UInt(4.W), Bool(), (a: UInt) => a.andR()),
    ("orr", UInt(4.W), Bool(), (a: UInt) => a.orR()),
    ("xorr", UInt(4.W), Bool(), (a: UInt) => a.xorR()),
  )

  /** Unary operations of type (Bool) => AsyncReset */
  val unaryOpsBA = Seq (
    ("asAsyncReset", Bool(), AsyncReset(), (a: Bool) => a.asAsyncReset()),
  )

}

object Main extends App {

  FIRRTLOps.binaryOpsUUU.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.BinaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.BinaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.binaryOpsUUB.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.BinaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.BinaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.unaryOpsUU.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.unaryOpsSU.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.unaryOpsUS.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.unaryOpsBC.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.unaryOpsUB.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

  FIRRTLOps.unaryOpsBA.foreach {
    case (name, gen_in, gen_out, f) =>
      (new ChiselStage).emitChirrtl(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build"))
      (new ChiselStage).emitVerilog(new Generators.UnaryOp(name, gen_in, gen_out, f), Array("-td", "build", "-o", s"$name.sfc.v"))
  }

}
