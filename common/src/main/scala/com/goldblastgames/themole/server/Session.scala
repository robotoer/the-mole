package com.goldblastgames.themole.server

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

import com.github.oetzi.echo.core.Event
import com.github.oetzi.echo.core.EventSource

import com.goldblastgames.themole.Player
import com.goldblastgames.themole.io.Connect
import com.goldblastgames.themole.io.Packet
import com.goldblastgames.themole.io.PacketSerialization._

class Session private(
  val port: Int,
  val players: Seq[Player],
  val module: Map[Player, Event[Packet]] => Map[Player, Event[Packet]]
) {

  val server = new EventSource[(String, (DataInputStream, DataOutputStream))] {
    Listener(port)
        .foreach { socket =>
          val out: DataOutputStream = new DataOutputStream(socket.getOutputStream)
          val in: DataInputStream = new DataInputStream(socket.getInputStream)

          println("Waiting for connection string...")
          val data = in.readUTF
          deserialize(data) match {
            case Connect(name) => {
              println("Player %s connected.".format(name))
              occur((name, (in, out)))
            }

            case x => {
              println("Invalid connection packet: %s".format(x.toString))
              sys.error("Received a bad connection packet: %s".format(x.toString))
            }
          }
        }
  }
  val inputStreams = server.map { (_, connection) =>
    val (name, (inputStream, _)) = connection

    (name, inputStream)
  }
  val outputStreams = server.map { (_, connection) =>
    val (name, (_, outputStream)) = connection

    (name, outputStream)
  }

  val inputs: Map[Player, PlayerInput] = players
      .map({ player =>
        val input = inputStreams
            .filter({ case (name, _) => name == player.name })
            .map((_, x) => x._2)

        (player, new PlayerInput(player.name, input))
      })
      .toMap

  val moduleOutputs = module(inputs)

  val outputs: Map[Player, PlayerOutput] = players
      .map({ player =>
        val output = outputStreams
            .filter({ case (name, _) => name == player.name })
            .map((_, x) => x._2)
        
        (player, new PlayerOutput(player.name, output, moduleOutputs(player)))
      })
      .toMap
}

object Session {
  def apply(port: Int, players: Seq[Player])(module: Map[Player, Event[Packet]] => Map[Player, Event[Packet]]) =
      new Session(port, players, module)
}
