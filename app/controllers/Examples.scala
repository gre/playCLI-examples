package controllers

import java.io._
import sys.process._

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.ws._
import playcli.CLI

/**
 * Declare all pipe and enumerate command we will use
 * Requires: curl ffmpeg sox convert find curl grep
 */
object Commands {
  // Pipe commands
  val grep = (q: String) => CLI.pipe(Seq("grep", q))
  val addEchoToOgg = CLI.pipe("sox -t ogg - -t ogg - echo 0.5 0.7 60 1")
  val scaleVideoHalf = CLI.pipe("ffmpeg -v warning -i pipe:0 -vf scale=iw/2:-1 -f avi pipe:1")
  val convertColors = (colors: Int) => CLI.pipe("convert - -colors "+colors+" png:-")
  val convert64colors = convertColors(64)

  // Enumerate comands
  val tail = (file: String) => CLI.enumerate(Seq("tail", "-f", file))
  val findAllInPath = (path: String) => CLI.enumerate(Seq("find", path))
  val curl = (url: String) => CLI.enumerate(Seq("curl", "-s", url))
}

object Resources {
  import Commands._
  import Utils._
  import codecs._
  import play.api.Play.current

  val oggWebRadioUrl = "http://radio.hbr1.com:19800/ambient.ogg"
  val mkvVideoUrl = "http://ftp.nluug.nl/pub/graphics/blender/demo/movies/Sintel.2010.1080p.mkv"

  val dictionaryFile = new File("/usr/share/dict/words")
  val mkvVideoFile = Play.getFile("Sintel.2010.1080p.mkv")
  val oggExampleFile = Play.getFile("public/resources/exemple.ogg")
  val nyancatFile = Play.getFile("public/resources/nyancat.jpg")

  //val (radioHeaders, radioStream) = AudioFormats.OggChunker(proxyBroadcast("http://radio.hbr1.com:19800/ambient.ogg") &> bytesFlattener)
  
  /* // TODO
  val (echoRadioHeaders, echoRadioStream) = {
    val radioStream = proxyUnicast(oggWebRadioUrl)
    val (headers, chunked) = OggChunker(radioStream &> addEchoToOgg &> bytesFlattener)
    val (broadcast, _) = Concurrent.broadcast(chunked)
    (headers, broadcast)
  }
  */

}


object Examples extends Controller {
  import Commands._
  import Resources._
  import Utils._

  // grep words
  def grepDictionary(search: String) = Action {
    val dict = Enumerator.fromFile(dictionaryFile)
    Ok.stream(dict &> grep(search))
      .withHeaders(CONTENT_TYPE -> "text/plain")
  }

  // Re-stream a web radio by adding echo with sox
  def webRadioWithEcho = Action {
    Ok.stream(proxy(oggWebRadioUrl)(addEchoToOgg &> _)) // TODO echoRadioHeaders >>> echoRadioStream
      .withHeaders(CONTENT_TYPE -> "audio/ogg")
  }

  // Retrieve an online video, resize it, stream it
  def downloadReEncodeAndStreamVideo = Action {
    Ok.stream(proxy(mkvVideoUrl)(scaleVideoHalf &> _))
      .withHeaders(CONTENT_TYPE -> "video/avi")
  }
  
  // Use a local video, resize it, stream it
  def reEncodeAndStreamVideo = Action {
    val stream = Enumerator.fromFile(mkvVideoFile)
    Ok.stream(stream &> scaleVideoHalf)
      .withHeaders(CONTENT_TYPE -> "video/avi")
  }

  def nyancatColorsQuantization = Action {
    Ok.stream(Enumerator.fromFile(nyancatFile) &> convertColors(14))
      .withHeaders(CONTENT_TYPE -> "image/png")
  }

  // Colors quantization example
  def colorsQuantization (url: String) = Action {
    Ok.stream(proxy(url)(convert64colors &> _))
      .withHeaders(CONTENT_TYPE -> "image/png")
  }

  // List all files in this Play project
  def find = Action {
    Ok.stream(findAllInPath(".") >>> Enumerator.eof)
      .withHeaders(CONTENT_TYPE -> "text/plain")
  }
  
  // Retrieve a webpage and display it
  def curlBlog = Action {
    Ok.stream(curl("http://blog.greweb.fr/") >>> Enumerator.eof)
      .withHeaders(CONTENT_TYPE -> "text/html")
  }

  // consume a ogg sound, add an echo effect and store in a temporary file
  def audioEchoEffectGenerate = Action {
    import concurrent.ExecutionContext.Implicits.global
    val file = File.createTempFile("sample_with_echo_", ".ogg") // handle myself the output
    val enum = Enumerator.fromFile(oggExampleFile)
    val addEchoToLocalOgg = CLI.consume(Process("sox -t ogg - -t ogg - echo 0.5 0.7 60 1") #> file)
    AsyncResult {
      enum |>>> addEchoToLocalOgg map { _ =>
        Ok("'"+file.getAbsolutePath+"' file has been generated.\n")
      }
    }
  }

}

object Utils {
  val bytesFlattener = Enumeratee.mapFlatten[Array[Byte]]( bytes => Enumerator.apply(bytes : _*) ) 

  // Consume a stream with url and push it in a socket with f
  // FIXME, how to tell WS to stop when socket is done?
  def proxy[A] (url: String)(f: Socket.Out[Array[Byte]] => Iteratee[Array[Byte], A]): Iteratee[Array[Byte], Unit] => Unit = 
    (socket: Socket.Out[Array[Byte]]) => WS.url(url).withTimeout(-1).get(headers => f(socket))

  // Proxify a stream forever
  def proxyBroadcast (url: String) : Enumerator[Array[Byte]] = {
    val (enumerator, channel) = Concurrent.broadcast[Array[Byte]]
    WS.url(url).withTimeout(-1).get(headers => Iteratee.foreach[Array[Byte]] { bytes => channel.push(bytes) })
    enumerator
  }
  
  // Proxify a stream forever
  def proxyUnicast (url: String) : Enumerator[Array[Byte]] = {
    Concurrent.unicast[Array[Byte]] { channel =>
      WS.url(url).withTimeout(-1).get(headers => Iteratee.foreach[Array[Byte]] { bytes => channel.push(bytes) })
    }
  }
}

