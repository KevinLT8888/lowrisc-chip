// See LICENSE.Cambridge for license details.

package freechips.rocketchip.lowrisc

import Chisel._
import freechips.rocketchip.config.{Config, Parameters}
import freechips.rocketchip.coreplex._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import scala.collection.mutable.ListBuffer

class LoRCBaseConfig extends Config(new BaseCoreplexConfig().alter((site,here,up) => {
  // DTS descriptive parameters
  case DTSModel => "freechips,rocketchip-unknown"
  case DTSCompat => Nil
  // External port parameters
  case IncludeJtagDTM => false
  case JtagDTMKey => new JtagDTMKeyDefault()
  case ExtMem => MasterPortParams(
    base = 0x80000000L,
    size = 0x10000000L,
    beatBytes = site(MemoryBusParams).beatBytes,
    idBits = 4)
  case ExPeriperals => ExPeriperalsParams(
    beatBytes = 8, // only support 64-bit right now
    idBits = 8,
    slaves = SlaveDevice.entries.toSeq
  )
  case ExtIn  => SlavePortParams(beatBytes = 8, idBits = 8, sourceBits = 4)
  // Additional device Parameters
  case ErrorParams => ErrorParams(Seq(AddressSet(0x3000, 0xfff)))
  case BootROMParams => BootROMParams(hang = 0x10000, contentFileName = "./bootrom/bootrom.img")
}))

class WithHost extends Config(Parameters.empty) {
  SlaveDevice.entries += ExSlaveParams(
    name       = "host",
    device     = () => new SimpleDevice("host", Seq()),
    base       = 0x40000000,
    size       = 0x00001000
  )
}

class WithUART extends Config(Parameters.empty) {
  SlaveDevice.entries +=  ExSlaveParams(
    name       = "uart",
    device     = () => new SimpleDevice("serial",Seq("xlnx,uart16550")),
    base       = 0x40002000,
    size       = 0x00002000,     // 8KB
    interrupts = 1
  )
}

class WithSPI extends Config(Parameters.empty) {
  SlaveDevice.entries +=  ExSlaveParams(
    name       = "spi",
    device     = () => new SimpleDevice("spi",Seq("xlnx,quad-spi")),
    base       = 0x40004000,
    size       = 0x00002000,     // 8KB
    interrupts = 1
  )
}

class WithBootRAM extends Config(Parameters.empty) {
  SlaveDevice.entries += ExSlaveParams(
    name       = "bram",
    device     = () => new SimpleDevice("bram", Seq("xlnx,bram")),
    base       = 0x41000000,
    size       = 0x00020000,     // 128KB
    resource   = Some("mem"),
    executable = true
  )
}

class WithFlash extends Config(Parameters.empty) {
  SlaveDevice.entries += ExSlaveParams(
    name       = "flash",
    device     = () => new SimpleDevice("flash", Seq("xlnx,flash")),
    base       = 0x42000000,
    size       = 0x01000000,     // 16M
    resource   = Some("mem"),
    executable = true
  )
}

class LoRCDefaultConfig extends Config(new WithHost ++ new WithNBigCores(1) ++ new LoRCBaseConfig)
class Nexys4Config extends Config(new WithUART ++ new WithBootRAM ++ new WithNBigCores(1) ++ new LoRCBaseConfig)
