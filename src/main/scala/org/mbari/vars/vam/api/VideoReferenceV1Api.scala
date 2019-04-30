/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
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
 */

package org.mbari.vars.vam.api

import java.net.URI
import java.util.UUID

import org.mbari.vars.vam.controllers.VideoReferenceController
import org.mbari.vars.vam.dao.jpa.{ ByteArrayConverter, VideoReference }
import org.scalatra.{ BadRequest, NoContent, NotFound }
import org.scalatra.swagger.{ DataType, ParamType, Parameter, Swagger }

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-06T16:27:00
 */
class VideoReferenceV1Api(controller: VideoReferenceController)(implicit val swagger: Swagger, val executor: ExecutionContext)
  extends APIStack {

  override protected def applicationDescription: String = "Video Reference API (v1)"

  val vsGET = (apiOperation[Iterable[VideoReference]]("findAll")
    summary "List all video-references")

  get("/?", operation(vsGET)) {
    controller.findAll.map(vs => controller.toJson(vs.asJava))
  }

  get("/uris") {

  }

  val uuidGET = (apiOperation[VideoReference]("findByUUID")
    summary "Find a video-reference by uuid"
    parameters (
      pathParam[UUID]("uuid").description("The UUID of the video-reference")))

  get("/:uuid", operation(uuidGET)) {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(s"""{not_found: "A video with a UUID of $uuid was not found in the database"}"""))
      case Some(v) => controller.toJson(v)
    })
  }

  get("/lastupdate/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None =>
        val error = Map("not_found" -> s"A video reference with a UUID of $uuid was not found in the database").asJava
        halt(NotFound(controller.toJson(error)))
      case Some(v) =>
        v.lastUpdated match {
          case None =>
            val error = Map(
              "missing_value" -> "No last updated timestamp was found",
              "video_reference_uuid" -> uuid).asJava
            halt(NotFound(controller.toJson(error)))
          case Some(t) =>
            val data = Map("timestamp" -> t.toString).asJava
            controller.toJson(data)
        }
    })
  }

  val uriGET = (apiOperation[VideoReference]("findByURI")
    summary "Find a video-reference by its URI"
    parameters (
      pathParam[URI]("uri").description("The URI of the video-reference")))

  get("/uri/:uri", operation(uriGET)) {
    val uri = params.getAs[URI]("uri").getOrElse(halt(BadRequest("Please provide a URI")))
    controller.findByURI(uri).map({
      case None => halt(NotFound(s"""{not_found: "A video with a URI of $uri was not found in the database"}"""))
      case Some(v) => controller.toJson(v)
    })
  }

  val shaGET = (apiOperation[VideoReference]("findBySha512")
    summary "Find a video-reference by checksum (SHA512)"
    parameters (
      pathParam[String]("sha512").description("The Base64 encoded SHA512 of the video-reference")))

  get("/sha512/:sha512", operation(shaGET)) {
    val sha = params.get("sha512")
      .map(s => ByteArrayConverter.decode(s))
      .getOrElse(halt(BadRequest("Please provide a Base64 encoded sha512 checksum")))
    controller.findBySha512(sha).map {
      case None => halt(NotFound(s"""{not_found: "A video with a SHA512 checksum of '$sha' was not found in the database"}"""))
      case Some(vr) => controller.toJson(vr)
    }
  }

  val vrDELETE = (apiOperation[Unit]("delete")
    summary "Delete a video-reference."
    parameters (
      pathParam[UUID]("uuid").description("The UUID of the video-reference to be deleted")))

  delete("/:uuid", operation(vrDELETE)) {
    validateRequest()
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("""{error: "A 'uuid' parameter is required"}""")))
    controller.delete(uuid).map({
      case true => halt(NoContent()) // Success! Deleted video with uuid
      case false => halt(NotFound(s"""{not_found: "A video with a UUID of $uuid was not found in the database"}"""))
    })
  }

  val vPOST = (apiOperation[String]("create")
    summary "Create a video-reference"
    parameters (
      Parameter("video_uuid", DataType.String, Some("The uuid of the owning video"), None, ParamType.Body, required = true),
      Parameter("uri", DataType.String, Some("The unique URI of the video"), None, ParamType.Body, required = true),
      Parameter("container", DataType.String, Some("The container mimetype"), None, ParamType.Body, required = false),
      Parameter("video_codec", DataType.String, Some("An identifier for the video codec"), None, ParamType.Body, required = false),
      Parameter("audio_codec", DataType.String, Some("An identifier for the audio codec"), None, ParamType.Body, required = false),
      Parameter("width", DataType.Int, Some("The video's width in pixels"), None, ParamType.Body, required = false),
      Parameter("height", DataType.Int, Some("The video's height in pixels"), None, ParamType.Body, required = false),
      Parameter("frame_rate", DataType.Double, Some("The frame-rate of the video in frames per second"), None, ParamType.Body, required = false),
      Parameter("size_bytes", DataType.Long, Some("The size of the video in bytes"), None, ParamType.Body, required = false),
      Parameter("description", DataType.String, Some("A description of the video"), None, ParamType.Body, required = false),
      Parameter("sha512", DataType.String, Some("The SHA512 checksum of the video (base 64 encoded)"), None, ParamType.Body, required = false)))

  post("/", operation(vPOST)) {
    validateRequest()
    val videoUUID = params.getAs[UUID]("video_uuid").getOrElse(halt(BadRequest("""{error: "A 'video_uuid' parameter is required"}""")))
    val uri = params.getAs[URI]("uri").getOrElse(halt(BadRequest("""{error: "A 'uri' parameter is required"}""")))
    val description = params.get("description")
    val container = params.get("container")
    val videoCodec = params.get("video_codec")
    val audioCodec = params.get("audio_codec")
    val width = params.getAs[Int]("width")
    val height = params.getAs[Int]("height")
    val frameRate = params.getAs[Double]("frame_rate")
    val sizeBytes = params.getAs[Long]("size_bytes")
    val sha512 = params.getAs[Array[Byte]]("sha512")
    controller.create(videoUUID, uri, container, videoCodec, audioCodec, width, height, frameRate,
      sizeBytes, description, sha512).map(controller.toJson)
  }

  val vPUT = (apiOperation[String]("update")
    summary "Update a video-reference"
    parameters (
      pathParam[UUID]("uuid").description("The UUID of the video-reference to be updated"),
      Parameter("video_uuid", DataType.String, Some("The uuid of the owning video"), None, ParamType.Body, required = false),
      Parameter("uri", DataType.String, Some("The unique URI of the video"), None, ParamType.Body, required = false),
      Parameter("container", DataType.String, Some("The container mimetype"), None, ParamType.Body, required = false),
      Parameter("video_codec", DataType.String, Some("An identifier for the video codec"), None, ParamType.Body, required = false),
      Parameter("audio_codec", DataType.String, Some("An identifier for the audio codec"), None, ParamType.Body, required = false),
      Parameter("width", DataType.Int, Some("The video's width in pixels"), None, ParamType.Body, required = false),
      Parameter("height", DataType.Int, Some("The video's height in pixels"), None, ParamType.Body, required = false),
      Parameter("frame_rate", DataType.Double, Some("The frame-rate of the video in frames per second"), None, ParamType.Body, required = false),
      Parameter("size_bytes", DataType.Long, Some("The size of the video in bytes"), None, ParamType.Body, required = false),
      Parameter("description", DataType.String, Some("A description of the video"), None, ParamType.Body, required = false),
      Parameter("sha512", DataType.String, Some("The SHA512 checksum of the video (base 64 encoded)"), None, ParamType.Body, required = false)))

  put("/:uuid", operation(vPUT)) {
    validateRequest()
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("""{error: "A 'uri' parameter is required"}""")))
    val videoUUID = params.getAs[UUID]("video_uuid")
    val uri = params.getAs[URI]("uri")
    val description = params.get("description")
    val container = params.get("container")
    val videoCodec = params.get("video_codec")
    val audioCodec = params.get("audio_codec")
    val width = params.getAs[Int]("width")
    val height = params.getAs[Int]("height")
    val frameRate = params.getAs[Double]("frame_rate")
    val sizeBytes = params.getAs[Long]("size_bytes")
    val sha512 = params.getAs[Array[Byte]]("sha512")
    controller.update(uuid, videoUUID, uri, container, videoCodec, audioCodec, width, height,
      frameRate, sizeBytes, description, sha512).map(controller.toJson)

  }

}
