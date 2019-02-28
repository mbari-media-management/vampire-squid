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
import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-05-06T15:43:00
 */
class VideoSequenceDAOSpec extends FlatSpec with Matchers {

  private[this] val daoFactory = DevelopmentTestDAOFactory

  private[this] val timeout = SDuration(2, TimeUnit.SECONDS)

  private[this] val dao = daoFactory.newVideoSequenceDAO()

  "VideoSequenceDAOImpl" should "create" in {

    var vs = VideoSequence("T12345", "Tiburon")
    Await.result(dao.runTransaction(d => d.create(vs)), timeout)

    val vs2 = dao.findByName(vs.name)
    vs2 shouldBe defined

  }

  var videoSequence: VideoSequence = _
  it should "create w/ child objects" in {
    var vs = VideoSequence("V05879", "Ventana")
    val d0 = Instant.parse("2016-04-01T00:15:00Z")
    vs.addVideo(Video("V20160401T001500", d0, Duration.ofMinutes(15)))
    val d1 = Instant.parse("2016-04-01T00:30:00Z")
    val v1 = Video("V20160401T003000", d1, Duration.ofMinutes(30))
    val vr1 = VideoReference(new URI("http://www.mbari.org/movies/test.mp4"))
    v1.addVideoReference(vr1)
    vs.addVideo(v1)

    Await.result(dao.runTransaction(d => d.create(vs)), timeout)
    val vs2 = dao.findByName(vs.name)
    vs2 shouldBe defined
    val vs3 = vs2.get
    vs3.videos.size should be(2)
    videoSequence = vs3
  }

  it should "fail to create VideoSequence with a name already stored in the database" in {
    val vs = VideoSequence(videoSequence.name, "Fubar")
    a[Exception] should be thrownBy {
      Await.result(dao.runTransaction(d => d.create(vs)), timeout)
    }

  }

  it should "update" in {
    val newCameraID = "Ventana"
    val updatedSequence = Await.result(dao.runTransaction(d => {
      val vs2 = d.findByName(videoSequence.name)
      vs2 shouldBe defined
      vs2.get.cameraID = newCameraID
      vs2
    }), timeout) // Changes made in transactions should be propagated

    updatedSequence shouldBe defined
    updatedSequence.get.name should be(videoSequence.name)
    updatedSequence.get.cameraID should be(newCameraID)

    val vs3 = Await.result(dao.runTransaction(d => d.findByName(videoSequence.name)), timeout)
    vs3 shouldBe defined
    dao.entityManager.detach(vs3.get)
    vs3.get.cameraID should be(newCameraID)

    vs3.get.cameraID = "Should not be persisted"

    val vs4 = dao.findByName(videoSequence.name)
    vs4 shouldBe defined
    vs4.get.cameraID should be(newCameraID)
    videoSequence = vs4.get
  }

  it should "insert child videos in the datastore" in {
    val name = "i2map 2009.123.04"
    val videoSequence = VideoSequence(name, "i2map")
    videoSequence.addVideo(Video("woah there nelly", Instant.now(), Duration.ofMinutes(15)))
    videoSequence.addVideo(Video("woah there nelly II", Instant.now(), Duration.ofMinutes(30)))
    Await.result(dao.runTransaction(d => d.create(videoSequence)), timeout)

    val vs = dao.findByName(name)
    vs shouldBe defined
    vs.get.videos.size should be(2)

    val v = Video("another one", Instant.now())
    vs.get.addVideo(v)
    Await.result(dao.runTransaction(d => d.update(vs.get)), timeout) // update should insert our new video

    val vs2 = dao.findByName(name)
    vs2 shouldBe defined
    vs2.get.videos.size should be(3)

  }

  var videoUUID: UUID = _
  it should "findByCameraID" in {
    val vs = Await.result(dao.runTransaction(d => d.findByCameraID(videoSequence.cameraID)), timeout)
    vs should have size 1
    val a = vs.head
    a.cameraID should be(videoSequence.cameraID)

    // Setup for later test. We need a videoUUID and UUID
    videoUUID = a.videos.head.uuid

  }

  it should "findByName" in {
    val vs = Await.result(dao.runTransaction(d => d.findByName(videoSequence.name)), timeout)
    vs shouldBe defined
    val a = vs.get
    a.name should be(videoSequence.name)
  }

  it should "findByVideoUUID" in {
    val vs = Await.result(dao.runTransaction(d => d.findByVideoUUID(videoSequence.videos.head.uuid)), timeout)
    vs shouldBe defined
    val a = vs.get
    println(s"!!!!! ${a.videos.size}")
    val v = a.videos.filter(_.uuid.equals(videoUUID))
    v should have size 1
    v.head.uuid should be(videoUUID)
  }

  it should "findByTimestamp" in {
    val timestamp = videoSequence.videos.head.start
    val duration = videoSequence.videos.head.duration
    val vs = Await.result(dao.runTransaction(d =>
      d.findByTimestamp(timestamp.plusMillis(duration.dividedBy(2).toMillis), duration)), timeout)
    vs should have size (1)
  }

  it should "findByNameAndTimestamp" in {
    val timestamp = videoSequence.videos.head.start
    val duration = videoSequence.videos.head.duration
    val vs = Await.result(dao.runTransaction(d =>
      d.findByNameAndTimestamp(videoSequence.name, timestamp.plusMillis(duration.dividedBy(2).toMillis), duration)), timeout)
    vs should have size (1)
  }

  it should "delete" in {
    val vs = dao.findByName(videoSequence.name)
    vs shouldBe defined
    Await.result(dao.runTransaction(d => d.delete(vs.get)), timeout)
    val vs1 = Await.result(dao.runTransaction(d => d.findByName(videoSequence.name)), timeout)
    vs1 shouldBe empty
  }

  it should "deleteByUUID" in {
    val name = "Brian's awesome AUV - 123456789"
    val videoSequence = VideoSequence(name, "awesome", Seq(Video("foo", Instant.now())))
    Await.result(dao.runTransaction(d => d.create(videoSequence)), timeout)

    val vs = dao.findByName(name)
    vs shouldBe defined

    Await.result(dao.runTransaction(d => d.deleteByUUID(vs.get.uuid)), timeout)

    val vs2 = dao.findByName(name)
    vs2 shouldBe empty

  }

  it should "delete all" in {
    val all = Await.result(dao.runTransaction(d => d.findAll()), timeout)
    Await.result(dao.runTransaction(d => {
      all.foreach(d.delete)
    }), timeout)
    val allGone = Await.result(dao.runTransaction(d => d.findAll()), timeout)
    allGone.size should be(0)
  }

  daoFactory.cleanup()

}
