//******************************************************************************
// Copyright (c) 2018 - 2019, The Regents of the University of California (Regents).
// All Rights Reserved. See LICENSE and LICENSE.SiFive for license details.
//------------------------------------------------------------------------------

package verif

import org.scalatest._

import chisel3._
import chisel3.util._

import freechips.rocketchip.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import freechips.rocketchip.subsystem._

/**
 * Factory object to help create a set of BOOM parameters to use in tests
 */
object VerifTestUtils {

  private def augment(tp: TileParams)(implicit p: Parameters): Parameters = p.alterPartial {
    case TileKey => tp

    case TileVisibilityNodeKey => TLEphemeralNode()(ValName("tile_master"))

    case LookupByHartId => lookupByHartId(Seq(tp))
  }

  private def lookupByHartId(tps: Seq[TileParams]) = {
    // return a new lookup hart
    new LookupByHartIdImpl {
      def apply[T <: Data](f: TileParams => Option[T], hartId: UInt): T =
        PriorityMux(tps.collect { case t if f(t).isDefined => (t.hartId.U === hartId) -> f(t).get })
    }
  }

  def getVerifParameters(configName: String, configPackage: String = "verif"): Parameters = {
    // get the full path to the config
    val fullConfigName = configPackage + "." + configName

    // get the default unmodified params
    val origParams: Parameters = try {
      (Class.forName(fullConfigName).newInstance.asInstanceOf[Config] ++ Parameters.empty)
    }
    catch {
      case e: java.lang.ClassNotFoundException =>
        throw new Exception(s"""Unable to find config "$fullConfigName".""", e)
    }

    // get the tile parameters
    val verifTileParams = origParams(TilesLocated(InSubsystem)) // this is a seq
    //verifTileParams(0).instantiate(origParams) -> ResourceBinding must be called from within a BindingScope

    // augment the parameters
    val outParams = augment(verifTileParams(0).tileParams)(origParams)

    //orgParams
    outParams
  }

  def getTraitVerifParameters: Parameters = {
    val origParams = Parameters.empty

    // augment the parameters
    implicit val p = origParams.alterPartial {
      case TileKey => TraitVerifTileParams
      case XLen => 64 // (TODO make this an argument)
      case PgLevels => 3 // TODO 3 if XLen is 64 else 2
      //case LookupByHartId => lookupByHartId(Seq(TraitVerifTileParams))
      case MaxHartIdBits => 1
      case SystemBusKey => SystemBusParams(
        beatBytes = 16, // FOR GEMMINI (make this an argument)
        blockBytes = 64 // TODO: is this the right value?
      )
    }

    def verifTLUBundleParams: TLBundleParameters = TLBundleParameters(addressBits = 64, dataBits = 64, sourceBits = 1,
      sinkBits = 1, sizeBits = 6,
      echoFields = Seq(), requestFields = Seq(), responseFields = Seq(),
      hasBCE = false)

    val dummyInNode = BundleBridgeSource(() => TLBundle(verifTLUBundleParams))
    val dummyOutNode = BundleBridgeSink[TLBundle]()

    val tlMasterXbar = LazyModule(new TLXbar)
    val visibilityNode = TLEphemeralNode()(ValName("tile_master"))

    visibilityNode :=* tlMasterXbar.node
    tlMasterXbar.node :=
      BundleBridgeToTL(TLClientPortParameters(Seq(TLClientParameters("bundleBridgeToTL")))) :=
      dummyInNode
    //TODO: maybe values here paramterized
    //NOTE: address mask needed to be 0xffffffff so that paddrBits was 32 and not 12 (mask 0xfff)
    dummyOutNode :=
      TLToBundleBridge(TLManagerPortParameters(Seq(TLManagerParameters(address = Seq(AddressSet(0x0, BigInt("ffffffff", 16))),
        supportsGet = TransferSizes(1, 64), supportsPutFull = TransferSizes(1,64))), 16)):=
      visibilityNode

    val outParams = p.alterPartial {
      case TileVisibilityNodeKey => visibilityNode
    }

    //orgParams
    outParams
  }
}
