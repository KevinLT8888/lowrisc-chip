// See LICENSE for license details.

package lowrisc_chip

import Chisel._
import uncore._
import rocket._
import rocket.Util._
import scala.math.max

class DefaultConfig extends ChiselConfig (
  topDefinitions = { (pname,site,here) => 
    type PF = PartialFunction[Any,Any]
    def findBy(sname:Any):Any = here[PF](site[Any](sname))(pname)
    pname match {
      //Memory Parameters
      case CacheBlockBytes => 64
      case CacheBlockOffsetBits => log2Up(here(CacheBlockBytes))
      case PAddrBits => 32
      case PgIdxBits => 12
      case PgLevels => 3 // Sv39
      case PgLevelBits => site(PgIdxBits) - log2Up(site(XLen)/8)
      case VPNBits => site(PgLevels) * site(PgLevelBits)
      case PPNBits => site(PAddrBits) - site(PgIdxBits)
      case VAddrBits => site(VPNBits) + site(PgIdxBits)
      case ASIdBits => 7
      case MIFTagBits => 8
      case MIFDataBits => 128

      // IO space
      case IOBaseAddr0 => UInt("hffff0000") // ffff_0000 : ffff_ffff
      case IOAddrMask0 => UInt("h0000ffff")
      case IOBaseAddr1 => UInt("hffffffff") // empty
      case IOAddrMask1 => UInt("h00000000")

      //Params used by all caches
      case NSets => findBy(CacheName)
      case NWays => findBy(CacheName)
      case RowBits => findBy(CacheName)
      case NTLBEntries => findBy(CacheName)

      //L1 I$
      case NBTBEntries => 62
      case NRAS => 2
      case "L1I" => {
        case NSets => Knob("L1I_SETS")
        case NWays => Knob("L1I_WAYS")
        case RowBits => 4*site(CoreInstBits)
        case NTLBEntries => 8
      }:PF

      //L1 D$
      case StoreDataQueueDepth => 17
      case ReplayQueueDepth => 16
      case NMSHRs => Knob("L1D_MSHRS")
      case LRSCCycles => 32 
      case "L1D" => {
        case NSets => Knob("L1D_SETS")
        case NWays => Knob("L1D_WAYS")
        case RowBits => 2*site(XLen)
        case NTLBEntries => 8
      }:PF
      case ECCCode => None
      case Replacer => () => new RandomReplacement(site(NWays))

      //L2 $
      case NAcquireTransactors => Knob("L2_XACTORS")
      case NSecondaryMisses => 4
      case L2DirectoryRepresentation => new FullRepresentation(site(TLNCachingClients))
      case "L2Bank" => {
        case NSets => Knob("L2_SETS")
        case NWays => Knob("L2_WAYS")
        case RowBits => site(TLDataBits)
      }: PF

      // Tag Cache
      case TagBits => 4
      case TCBlockBits => site(MIFDataBits)
      case TCTransactors => Knob("TC_XACTORS")
      case TCBlockTags => 1 << log2Down(site(TCBlockBits) / site(TagBits))
      case TCBaseAddr => Knob("TC_BASE_ADDR")
      case "TagCache" => {
        case NSets => Knob("TC_SETS")
        case NWays => Knob("TC_WAYS")
        case RowBits => site(TCBlockTags) * site(TagBits)
      }: PF
      
      //Tile Constants
      case NTiles => Knob("NTILES")
      case NDCachePorts => 2
      case NPTWPorts => 2
      case BuildRoCC => None

      //Rocket Core Constants
      case FetchWidth => 1
      case RetireWidth => 1
      case UseVM => true
      case FastLoadWord => true
      case FastLoadByte => false
      case FastMulDiv => true
      case XLen => 64
      case NMultXpr => 32
      case BuildFPU => Some(() => Module(new FPU))
      case FDivSqrt => true
      case SFMALatency => 2
      case DFMALatency => 3
      case CoreInstBits => 32
      case CoreDCacheReqTagBits => 7 + log2Up(site(NDCachePorts))
      case NCustomMRWCSRs => 0
      
      //Uncore Paramters
      case NBanks => Knob("NBANKS")
      case BankIdLSB => 0
      case LNHeaderBits => log2Up(max(site(TLNManagers),site(TLNClients)))
      case TLBlockAddrBits => site(PAddrBits) - site(CacheBlockOffsetBits)
      case TLNClients => site(TLNCachingClients) + site(TLNCachelessClients)
      case TLDataBits => site(CacheBlockBytes)*8/site(TLDataBeats)
      case TLDataBeats => 4
      case TLNetworkIsOrderedP2P => false
      case TLNManagers => findBy(TLId)
      case TLNCachingClients => findBy(TLId)
      case TLNCachelessClients => findBy(TLId)
      case TLCoherencePolicy => findBy(TLId)
      case TLMaxManagerXacts => findBy(TLId)
      case TLMaxClientXacts => findBy(TLId)
      case TLMaxClientsPerPort => findBy(TLId)
      
      case "L1ToL2" => {
        case TLNManagers => site(NBanks)
        case TLNCachingClients => site(NTiles)
        case TLNCachelessClients => site(NTiles)
        case TLCoherencePolicy => new MESICoherence(site(L2DirectoryRepresentation)) 
        case TLMaxManagerXacts => site(NAcquireTransactors) + 2  // ?? + 2
        case TLMaxClientXacts => site(NMSHRs)
        case TLMaxClientsPerPort => 1
      }:PF
      case "L2ToTC" => {
        case TLNManagers => 1
        case TLNCachingClients => site(NBanks)
        case TLNCachelessClients => 0
        case TLCoherencePolicy => new MEICoherence(new NullRepresentation(site(NBanks)))
        case TLMaxManagerXacts => site(TCTransactors) // ?? + ?
        case TLMaxClientXacts => 1
        case TLMaxClientsPerPort => site(NAcquireTransactors) + 2
      }:PF
      
    }},
  knobValues = {
    case "NTILES" => 1
    case "NBANKS" => 1

    case "L1D_MSHRS" => 2
    case "L1D_SETS" => 64
    case "L1D_WAYS" => 4

    case "L1I_SETS" => 64
    case "L1I_WAYS" => 4

    case "L2_XACTORS" => 2
    case "L2_SETS" => 1024
    case "L2_WAYS" => 8

    case "TC_XACTORS" => 1
    case "TC_SETS" => 64
    case "TC_WAYS" => 8
    case "TC_BASE_ADDR" => 15 << 28 // 0xf000_0000
  }
)


class FPGAConfig extends ChiselConfig (
  knobValues = {
    case "TC_BASE_ADDR" => 15 << 24 // 0xf00,0000
  }
)
