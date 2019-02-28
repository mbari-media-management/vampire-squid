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

import java.sql.Timestamp
import java.time.{ Duration, Instant }
import java.util.{ Date, UUID }
import javax.persistence.EntityManager

import org.mbari.vars.vam.Constants
import org.mbari.vars.vam.dao.VideoDAO

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-05-11T14:35:00
 */
class VideoDAOImpl(entityManager: EntityManager)
  extends BaseDAO[Video](entityManager)
  with VideoDAO[Video] {

  override def findByUUID(uuid: UUID): Option[Video] =
    findByNamedQuery("Video.findByUuid", Map("uuid" -> uuid))
      .headOption

  override def findByName(name: String): Option[Video] =
    findByNamedQuery("Video.findByName", Map("name" -> name)).headOption

  override def findByVideoSequenceUUID(uuid: UUID): Iterable[Video] =
    findByNamedQuery("Video.findByVideoSequenceUUID", Map("uuid" -> uuid))

  override def findByVideoReferenceUUID(uuid: UUID): Option[Video] =
    findByNamedQuery("Video.findByVideoReferenceUUID", Map("uuid" -> uuid))
      .headOption

  override def findByTimestamp(timestamp: Instant, window: Duration = Constants.DEFAULT_DURATION_WINDOW): Iterable[Video] = {
    val halfRange = window.dividedBy(2)
    val startDate = timestamp.minus(halfRange)
    val endDate = timestamp.plus(halfRange)
    val videos = findByNamedQuery("Video.findBetweenDates", Map("startDate" -> startDate, "endDate" -> endDate))
    val hasTimestamp = containsTimestamp(_: Video, timestamp)
    videos.filter(hasTimestamp)
  }

  override def findBetweenTimestamps(t0: Instant, t1: Instant): Iterable[Video] =
    findByNamedQuery("Video.findBetweenDates", Map("startDate" -> t0, "endDate" -> t1))

  override def findAll(): Iterable[Video] = findByNamedQuery("Video.findAll")

  override def deleteByUUID(primaryKey: UUID): Unit = {
    val video = findByUUID(primaryKey)
    video.foreach(v => delete(v))
  }

  private def containsTimestamp(video: Video, timestamp: Instant): Boolean = {
    val startDate = video.start
    val endDate = video.start.plus(video.duration)

    startDate.equals(timestamp) ||
      endDate.equals(timestamp) ||
      (startDate.isBefore(timestamp) && endDate.isAfter(timestamp))
  }

  override def findAllNames(): Iterable[String] =
    entityManager.createNamedQuery("Video.findAllNames")
      .getResultList
      .asScala
      .map(_.toString)

  override def findAllNamesAndTimestamps(): Iterable[(String, Instant)] =
    entityManager.createNamedQuery("Video.findAllNamesAndStartDates")
      .getResultList
      .asScala
      .map(r => r.asInstanceOf[Array[Any]])
      .map(r => r(0).toString -> r(1).asInstanceOf[Timestamp].toInstant)

  def findNamesByVideoSequenceName(videoSequenceName: String): Iterable[String] = {
    val query = entityManager.createNamedQuery("Video.findNamesByVideoSequenceName")
    query.setParameter(1, videoSequenceName)
    query.getResultList
      .asScala
      .map(_.toString)
  }

}
