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

package org.mbari.vars.vam.dao.jpa

import java.net.URI
import javax.activation.MimeType
import javax.persistence.{ EntityListeners, Table, _ }

import com.google.gson.annotations.{ Expose, SerializedName }

import scala.util.Try

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-05-05T18:19:00
 */
@Entity(name = "VideoReference")
@Table(name = "video_references", indexes = Array(
  new Index(name = "idx_video_references__uri", columnList = "uri"),
  new Index(name = "idx_video_references__video_id", columnList = "video_id")))
@EntityListeners(value = Array(classOf[TransactionLogger], classOf[UUIDKeyGenerator]))
@NamedNativeQueries(Array(
  new NamedNativeQuery(
    name = "VideoReference.findByFileName",
    query = "SELECT uuid FROM video_references WHERE uri LIKE ?1")))
@NamedQueries(
  Array(
    new NamedQuery(
      name = "VideoReference.findByUuid",
      query = "SELECT v FROM VideoReference v WHERE v.uuid = :uuid"),
    new NamedQuery(
      name = "VideoReference.findAll",
      query = "SELECT v FROM VideoReference v"),
    new NamedQuery(
      name = "VideoReference.findBySha512",
      query = "SELECT v FROM VideoReference v WHERE v.sha512 = :sha512"),
    new NamedQuery(
      name = "VideoReference.findByVideoUUID",
      query = "SELECT v FROM VideoReference v JOIN v.video w WHERE w.uuid = :uuid"),
    new NamedQuery(
      name = "VideoReference.findByURI",
      query = "SELECT v FROM VideoReference v WHERE v.uri = :uri")))
class VideoReference extends HasID
  with HasUUID
  with HasOptimisticLock
  with HasDescription {

  @Expose(serialize = true)
  @Basic(optional = false)
  @Column(
    name = "uri",
    unique = true,
    length = 1024,
    nullable = false)
  @Convert(converter = classOf[URIConverter])
  var uri: URI = _

  /**
   * Defines the video files container. We are using mimetypes to provide
   * container definitions. Note that the mimetype does not always indicate
   * the video/audio encoding
   */
  @Expose(serialize = true)
  @Column(
    name = "container",
    length = 128)
  var container: String = _

  @Expose(serialize = true)
  @Column(
    name = "video_codec",
    length = 128)
  var videoCodec: String = _

  @Expose(serialize = true)
  @Column(
    name = "audio_codec",
    length = 128)
  var audioCodec: String = _

  @Expose(serialize = true)
  @Column(name = "width")
  var width: Int = _

  @Expose(serialize = true)
  @Column(name = "height")
  var height: Int = _

  @Expose(serialize = true)
  @Column(
    name = "frame_rate")
  var frameRate: Double = _

  @Expose(serialize = true)
  @SerializedName(value = "size_bytes")
  @Column(
    name = "size_bytes")
  var size: Long = _

  @Expose(serialize = false)
  @ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.DETACH), optional = false)
  @JoinColumn(name = "video_id", nullable = false)
  var video: Video = _

  def mimetype: Option[MimeType] = Try(new MimeType(container)).toOption

  // Checksum allows reverse lookups. Store checksum as hex
  @Expose(serialize = true)
  @Column(name = "sha512", length = 128, nullable = true, unique = true)
  @Convert(converter = classOf[ByteArrayConverter])
  var sha512: Array[Byte] = _

  override def toString: String = s"VideoReference($uri)"

}

object VideoReference {

  def apply(
    uri: URI,
    container: Option[String] = None,
    videoCodec: Option[String] = None,
    audioCodec: Option[String] = None,
    width: Option[Int] = None,
    height: Option[Int] = None,
    frameRate: Option[Double] = None,
    sizeBytes: Option[Long] = None,
    description: Option[String] = None,
    sha512: Option[Array[Byte]] = None): VideoReference = {
    val videoReference = new VideoReference
    videoReference.uri = uri
    container.foreach(v => videoReference.container = v)
    videoCodec.foreach(v => videoReference.videoCodec = v)
    audioCodec.foreach(v => videoReference.audioCodec = v)
    width.foreach(v => videoReference.width = v)
    height.foreach(v => videoReference.height = v)
    frameRate.foreach(v => videoReference.frameRate = v)
    sizeBytes.foreach(v => videoReference.size = v)
    description.foreach(v => videoReference.description = v)
    sha512.foreach(videoReference.sha512 = _)
    videoReference
  }

  def apply(uri: URI): VideoReference = apply(uri, container = None)

  def apply(
    uri: URI,
    container: String,
    videoCodec: String,
    audioCodec: String,
    width: Int,
    height: Int): VideoReference =
    apply(uri, Some(container), Some(videoCodec), Some(audioCodec),
      Some(width), Some(height))

  def apply(
    uri: URI,
    container: String,
    videoCodec: String,
    audioCodec: String,
    width: Int,
    height: Int,
    description: String): VideoReference =
    apply(uri, Some(container), Some(videoCodec), Some(audioCodec),
      Some(width), Some(height), description = Some(description))

  def apply(
    uri: URI,
    container: String,
    videoCodec: String,
    audioCodec: String,
    width: Int,
    height: Int,
    description: String,
    sha512: Array[Byte]): VideoReference =
    apply(uri, Some(container), Some(videoCodec), Some(audioCodec),
      Some(width), Some(height), description = Some(description), sha512 = Some(sha512))

  def apply(
    uri: URI,
    container: String,
    videoCodec: String,
    audioCodec: String,
    width: Int,
    height: Int,
    frameRate: Double,
    size: Long): VideoReference =
    apply(uri, Some(container), Some(videoCodec), Some(audioCodec),
      Some(width), Some(height), frameRate = Some(frameRate), sizeBytes = Some(size))

  def apply(
    uri: URI,
    container: String,
    videoCodec: String,
    audioCodec: String,
    width: Int,
    height: Int,
    frameRate: Double,
    size: Long,
    description: String): VideoReference =
    apply(uri, Some(container), Some(videoCodec), Some(audioCodec),
      Some(width), Some(height), frameRate = Some(frameRate), sizeBytes = Some(size),
      description = Some(description))

}
