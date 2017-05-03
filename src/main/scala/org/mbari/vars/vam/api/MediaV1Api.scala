package org.mbari.vars.vam.api

import java.net.URI
import java.time.{ Duration, Instant }

import org.mbari.vars.vam.controllers.MediaController
import org.mbari.vars.vam.dao.jpa.ByteArrayConverter
import org.scalatra.{ BadRequest, NotFound }
import org.scalatra.swagger.Swagger

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

/**
 * @author Brian Schlining
 * @since 2017-03-06T17:08:00
 */
class MediaV1Api(controller: MediaController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "Media API (v1)"

  override protected val applicationName: Option[String] = Some("MediaAPI")

  post("/") {
    validateRequest()
    val videoSequenceName = params.get("video_sequence_name")
      .getOrElse(halt(BadRequest("{}", reason = "A 'video_sequence_name' param is required")))
    val cameraId = params.get("camera_id")
      .getOrElse(halt(BadRequest("{}", reason = "A 'camera_id' param is required")))
    val videoName = params.get("video_name")
      .getOrElse(halt(BadRequest("{}", reason = "A 'video_name' param is required")))
    val uri = params.getAs[URI]("uri")
      .getOrElse(halt(BadRequest(body = "{}", reason = "A 'uri' parameters is required.")))
    val start = params.getAs[Instant]("start_timestamp")
      .getOrElse(halt(BadRequest("{}", reason = "A 'start_timestamp' param is required")))
    val duration = params.getAs[Duration]("duration_millis")
    val container = params.get("container")
    val videoCodec = params.get("video_codec")
    val audioCodec = params.get("audio_codec")
    val width = params.getAs[Int]("width")
    val height = params.getAs[Int]("height")
    val frameRate = params.getAs[Double]("frame_rate")
    val sizeBytes = params.getAs[Long]("size_bytes")
    val videoRefDescription = params.get("video_description")
    val sha512 = params.getAs[Array[Byte]]("sha512")
    validateRequest()
    controller.create(
      videoSequenceName,
      cameraId,
      videoName,
      uri,
      start,
      duration,
      container,
      videoCodec,
      audioCodec,
      width,
      height,
      frameRate,
      sizeBytes,
      videoRefDescription,
      sha512).map(controller.toJson)
  }

  get("/sha512/:sha512") {
    val shaString = params.get("sha512")
      .getOrElse(halt(BadRequest("{}", reason = "A hex encoded SHA512 checksum is required")))
    val sha = ByteArrayConverter.decode(shaString)
    controller.findBySha512(sha).map({
      case None => halt(NotFound("{}", reason = s"A video with matching checksum was not found"))
      case Some(v) => controller.toJson(v)
    })
  }

  get("/videosequence/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("{}", reason = "A video sequence name parameter is required")))
    controller.findByVideoSequenceName(name)
      .map(_.asJava)
      .map(controller.toJson)
  }

  get("/video/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("{}", reason = "A video name parameter is required")))
    controller.findByVideoName(name)
      .map(_.asJava)
      .map(controller.toJson)
  }

  get("/camera/:camera_id/:datetime") {
    val cameraId = params.get("camera_id")
      .getOrElse(halt(BadRequest("{}", reason = "A 'camera id' parameters is required")))
    val ts = params.getAs[Instant]("datetime")
      .getOrElse(halt(BadRequest("", reason = "A datetime parameter in UTC time formated as 'yyyy-mm-ddThh:MM:ssZ' is required")))
    controller.findByCameraIdAndTimestamp(cameraId, ts)
      .map(_.asJava)
      .map(controller.toJson)
  }

}