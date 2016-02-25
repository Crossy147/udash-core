package io.udash.rpc

import com.typesafe.scalalogging.LazyLogging
import org.atmosphere.cpr.{AtmosphereResource, DefaultAtmosphereResourceSessionFactory}

import scala.util.Try

/**
  * <p>Default [[io.udash.rpc.AtmosphereServiceConfig]] implementation.</p>
  *
  * <p>Creates RPC endpoint per HTTP connection. Endpoint can be aware of [[io.udash.rpc.ClientId]]. </p>
  */
final class DefaultAtmosphereServiceConfig[ServerRPCType <: RPC](localRpc: (ClientId) => ExposesServerRPC[ServerRPCType])
  extends AtmosphereServiceConfig[ServerRPCType] with LazyLogging {

  private val RPCName = "RPC"
  private val connections = new DefaultAtmosphereResourceSessionFactory

  override def resolveRpc(resource: AtmosphereResource): ExposesServerRPC[ServerRPCType] =
    connections.getSession(resource).getAttribute(RPCName).asInstanceOf[ExposesServerRPC[ServerRPCType]]

  override def initRpc(resource: AtmosphereResource): Unit = synchronized {
    val session = connections.getSession(resource)

    if (session.getAttribute(RPCName) == null) {
      session.setAttribute(RPCName, localRpc(ClientId(resource.uuid())))
    }
  }

  override def filters: Seq[(AtmosphereResource) => Try[Any]] = List()

  override def onClose(resource: AtmosphereResource): Unit = {}
}