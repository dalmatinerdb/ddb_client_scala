package dalmatinerdb.client

import scala.util.{Try, Success, Failure}

sealed trait ReadRepairOption { def value: Int }
object ReadRepairOption {
  case object Off extends ReadRepairOption { val value = 0 }
  case object On extends ReadRepairOption { val value = 1 }
  case object Default extends ReadRepairOption { val value = 2 }

  def apply(s: String): ReadRepairOption = s.toLowerCase match {
    case "on" => ReadRepairOption.On
    case "off" => ReadRepairOption.Off
    case _ => ReadRepairOption.Default
  }
}

sealed trait ReadQuorumOption { def value: Int }
object ReadQuorumOption {
  case object Default extends ReadQuorumOption { val value = 0 }
  case object N extends ReadQuorumOption { val value = 255 }
  case class R(val value: Int) extends ReadQuorumOption

  def apply(s: String): ReadQuorumOption = s.toLowerCase match {
    case "n" => ReadQuorumOption.N
    case "default" => ReadQuorumOption.Default
    case rs => Try(rs.toInt) match {
      case Success(r) => ReadQuorumOption.R(r)
      case Failure(_) => ReadQuorumOption.Default
    }
  }
}

case class ReadOptions(val readRepair: ReadRepairOption,
                       val quorum: ReadQuorumOption)


