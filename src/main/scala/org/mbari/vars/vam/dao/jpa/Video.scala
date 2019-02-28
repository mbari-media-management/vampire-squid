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

import java.time.{ Duration, Instant }
import java.util.{ ArrayList => JArrayList, List => JList }
import javax.persistence.{ CascadeType, _ }

import com.google.gson.annotations.{ Expose, SerializedName }

import scala.collection.JavaConverters._

/**
 * A Video is an abstract representation of a discrete segment of video.
 *
 * @author Brian Schlining
 * @since 2016-05-05T17:54:00
 */
@Entity(name = "Video")
@Table(name = "videos", indexes = Array(
  new Index(name = "idx_videos__name", columnList = "name"),
  new Index(name = "idx_videos__start_time", columnList = "start_time"),
  new Index(name = "idx_videos__video_sequence_id", columnList = "video_sequence_id")))
@EntityListeners(value = Array(classOf[TransactionLogger], classOf[UUIDKeyGenerator]))
@NamedNativeQueries(Array(
  new NamedNativeQuery(
    name = "Video.findAllNames",
    query = "SELECT name FROM videos ORDER BY name"),
  new NamedNativeQuery(
    name = "Video.findNamesByVideoSequenceName",
    query = "SELECT v.name FROM videos v LEFT JOIN video_sequences vs ON v.video_sequence_id = vs.id WHERE vs.name = ?1 ORDER BY v.name ASC"),
  new NamedNativeQuery(
    name = "Video.findAllNamesAndStartDates",
    query = "SELECT name, start_time FROM videos ORDER BY start_time")))
@NamedQueries(Array(
  new NamedQuery(
    name = "Video.findAll",
    query = "SELECT v FROM Video v ORDER BY v.start"),
  new NamedQuery(
    name = "Video.findByName",
    query = "SELECT v FROM Video v WHERE v.name = :name"),
  new NamedQuery(
    name = "Video.findByUuid",
    query = "SELECT v FROM Video v WHERE v.uuid = :uuid"),
  new NamedQuery(
    name = "Video.findByVideoReferenceUUID",
    query = "SELECT v FROM Video v LEFT JOIN v.javaVideoReferences w WHERE w.uuid = :uuid"),
  new NamedQuery(
    name = "Video.findByVideoSequenceUUID",
    query = "SELECT v FROM Video v JOIN v.videoSequence w WHERE w.uuid = :uuid"),
  new NamedQuery(
    name = "Video.findBetweenDates",
    query = "SELECT v FROM Video v WHERE v.start >= :startDate AND v.start <= :endDate")))
class Video extends HasID
  with HasUUID
  with HasOptimisticLock
  with HasDescription {

  @Expose(serialize = true)
  @Basic(optional = false)
  @Column(
    name = "name",
    nullable = false,
    length = 512,
    unique = true)
  var name: String = _

  @Expose(serialize = true)
  @SerializedName(value = "start_timestamp")
  @Basic(optional = false)
  @Column(
    name = "start_time",
    nullable = false)
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  var start: Instant = _

  @Expose(serialize = true)
  @SerializedName(value = "duration_millis")
  @Column(
    name = "duration_millis",
    nullable = true)
  var duration: Duration = _

  @Expose(serialize = false)
  @ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.DETACH), optional = false)
  @JoinColumn(name = "video_sequence_id", nullable = false)
  var videoSequence: VideoSequence = _

  @Expose(serialize = true)
  @SerializedName(value = "video_references")
  @OneToMany(
    targetEntity = classOf[VideoReference],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "video",
    orphanRemoval = true)
  protected var javaVideoReferences: JList[VideoReference] = new JArrayList[VideoReference]

  def addVideoReference(videoView: VideoReference): Unit = {
    javaVideoReferences.add(videoView)
    videoView.video = this
  }

  def removeVideoReference(videoView: VideoReference): Unit = {
    javaVideoReferences.remove(videoView)
    videoView.video = null
  }

  def videoReferences: Seq[VideoReference] = javaVideoReferences.asScala

  override def toString = s"Video($name, $start)"
}

object Video {

  def apply(name: String, start: Instant): Video = {
    val v = new Video
    v.name = name
    v.start = start
    v
  }

  def apply(name: String, start: Instant, duration: Duration): Video = {
    val v = new Video
    v.name = name
    v.start = start
    v.duration = duration
    v
  }

  def apply(name: String, start: Instant, videoReferences: Iterable[VideoReference]): Video = {
    val v = new Video
    v.name = name
    v.start = start
    videoReferences.foreach(v.addVideoReference)
    v
  }

  def apply(name: String, start: Instant, duration: Option[Duration], videoReferences: Iterable[VideoReference]): Video = {
    val v = new Video
    v.name = name
    v.start = start
    duration.foreach(v.duration = _)
    videoReferences.foreach(v.addVideoReference)
    v
  }

  def apply(name: String, start: Instant, duration: Option[Duration], description: Option[String]) = {
    val v = new Video
    v.name = name
    v.start = start
    duration.foreach(v.duration = _)
    description.foreach(d => v.description = d)
    v
  }

}
