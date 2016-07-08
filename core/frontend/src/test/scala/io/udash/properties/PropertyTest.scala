package io.udash.properties

import io.udash.testing.UdashFrontendTest

import scala.collection.mutable
import scala.util.Random

class PropertyTest extends UdashFrontendTest {
  case class C(i: Int, s: String)

  trait TT {
    def i: Int
    def s: Option[String]
    def t: ST
  }

  trait ST {
    def c: C
    def s: Seq[Char]
  }

  sealed trait T
  case object TO1 extends T
  case object TO2 extends T
  case class TC1(i: Int) extends T
  case class TC2(s: String) extends T

  def newTT(iv: Int, sv: Option[String], cv: C, ssv: Seq[Char]) = new TT {
    override def i: Int = iv
    override def s: Option[String] = sv
    override def t: ST = new ST {
      override def c: C = cv
      override def s: Seq[Char] = ssv
    }
  }

  def randTT() = newTT(Random.nextInt(20), Some(Random.nextString(5)), C(Random.nextInt(20), Random.nextString(5)), Random.nextString(20))

  "Property" should {
    "update value" in {
      val p = Property[Int](5)
      val tp = Property[T](TO1)
      val cp = Property[C](C(1, "asd"))

      p.get should be(5)
      for (i <- Range(-200000, 200000, 666)) {
        p.set(i)
        p.get should be(i)
      }

      tp.get should be(TO1)
      tp.set(TC1(12))
      tp.get should be(TC1(12))
      tp.set(TO2)
      tp.get should be(TO2)

      cp.get should be(C(1, "asd"))
      cp.set(C(12, "asd2"))
      cp.get should be(C(12, "asd2"))
    }

    "fire listeners on value change" in {
      val values = mutable.ArrayBuffer[Any]()
      val listener = (v: Any) => values += v

      val p = Property[Int](5)
      val tp = Property[T](TO1)
      val cp = Property[C](C(1, "asd"))

      p.listen(listener)
      tp.listen(listener)
      cp.listen(listener)

      p.set(7)
      p.set(-321)
      p.set(-321)
      p.set(-321)
      p.set(-321)
      tp.set(TC1(12))
      tp.set(TO2)
      cp.set(C(12, "asd2"))

      values.size should be(5)
      values(0) should be(7)
      values(1) should be(-321)
      values(2) should be(TC1(12))
      values(3) should be(TO2)
      values(4) should be(C(12, "asd2"))
    }

    "transform and synchronize value" in {
      val values = mutable.ArrayBuffer[Any]()
      val listener = (v: Any) => values += v

      val cp = Property[C](C(1, "asd"))
      val tp = cp.transform[(T, T)](
        (c: C) => Tuple2(TC1(c.i), TC2(c.s)),
        (t: (T, T)) => t match {
          case (TC1(i), TC2(s)) => C(i, s)
          case _ => C(0, "")
        }
      )

      tp.listen(listener)
      cp.listen(listener)

      cp.get should be(C(1, "asd"))
      tp.get should be(Tuple2(TC1(1), TC2("asd")))

      cp.set(C(12, "asd2"))
      cp.get should be(C(12, "asd2"))
      tp.get should be(Tuple2(TC1(12), TC2("asd2")))

      tp.set(Tuple2(TC1(-5), TC2("tp")))
      cp.get should be(C(-5, "tp"))
      tp.get should be(Tuple2(TC1(-5), TC2("tp")))

      values.size should be(4)
      values should contain(C(12, "asd2"))
      values should contain(Tuple2(TC1(12), TC2("asd2")))
      values should contain(Tuple2(TC1(-5), TC2("tp")))
      values should contain(C(-5, "tp"))
    }
  }

  "ModelProperty" should {
    "update value and provide access to subproperties" in {
      val p = ModelProperty[TT]

      p.set(newTT(5, Some("s"), C(123, "asd"), Seq('a', 'b', 'c')))

      p.get.i should be(5)
      p.get.s should be(Some("s"))
      p.get.t.c should be(C(123, "asd"))
      p.get.t.s.size should be(3)

      p.subProp(_.i).set(42)
      p.get.i should be(42)

      p.subModel(_.t).subSeq(_.s).insert(0, 'e')
      p.get.t.s.size should be(4)

      p.subSeq(_.t.s).insert(0, 'f')
      p.get.t.s.size should be(5)

      p.subProp(_.t.c).set(C(321, "dsa"))
      p.get.t.c should be(C(321, "dsa"))
    }

    "fire listeners on value change" in {
      val values = mutable.ArrayBuffer[Any]()
      val listener = (v: Any) => values += v

      val p = ModelProperty[TT]
      p.listen(listener)

      p.set(newTT(5, Some("s"), C(123, "asd"), Seq('a', 'b', 'c')))
      values.size should be(1)

      p.subProp(_.i).set(42)
      p.subProp(_.i).set(42)
      p.subProp(_.i).set(42)
      values.size should be(2)

      p.subModel(_.t).subSeq(_.s).insert(0, 'e')
      values.size should be(3)

      p.subSeq(_.t.s).insert(0, 'f')
      values.size should be(4)

      p.subProp(_.t.c).set(C(321, "dsa"))
      values.size should be(5)

      CallbackSequencer.sequence {
        p.subSeq(_.t.s).insert(0, 'g')
        p.subSeq(_.t.s).insert(0, 'h')
        p.subSeq(_.t.s).insert(0, 'i')
        p.subProp(_.i).set(123)
      }
      values.size should be(6)
    }

    "transform and synchronize value" in {
      val values = mutable.ArrayBuffer[Any]()
      val listener = (v: Any) => values += v

      val p = ModelProperty[TT]
      val t = p.transform[Int](
        (p: TT) => p.i + p.t.c.i,
        (x: Int) => newTT(x/2, None, C(x/2, ""), Seq.empty)
      )

      p.listen(listener)
      t.listen(listener)

      p.set(newTT(5, Some("s"), C(123, "asd"), Seq('a', 'b', 'c')))
      t.get should be(123+5)

      t.set(64)
      p.get.i should be(32)
      p.get.t.c.i should be(32)

      values.size should be(4)
      values should contain(64)
      values should contain(128)
    }
  }

  "SeqProperty" should {
    "handle sequence of properties" in {
      val p = SeqProperty[Int](Seq(1,2,3))
      val pt = SeqProperty[T](TO1, TC1(5), TO2)
      val ptt = SeqProperty[TT](randTT(), randTT(), randTT())

      def checkProperties(expectedSize: Int, props: Seq[SeqProperty[_, Property[_]]] = Seq(p, pt, ptt)) = {
        props.foreach(p => {
          p.get.size should be(expectedSize)
          p.get should be(p.elemProperties.map(_.get))
        })
      }

      checkProperties(expectedSize = 3)

      // Replace part
      var nextTT = randTT()
      p.replace(0, 2, 0)
      pt.replace(0, 2, TO2)
      ptt.replace(0, 2, nextTT)

      checkProperties(expectedSize = 2)
      p.get.head should be(0)
      pt.get.head should be(TO2)
      ptt.get.head.i should be(nextTT.i)
      ptt.get.head.s should be(nextTT.s)
      ptt.get.head.t.c should be(nextTT.t.c)

      // Replace whole
      nextTT = randTT()
      p.replace(0, 2, 1, 2, 3, 4)
      pt.replace(0, 2, TO1, TC1(1), TC2("asd"), TO2)
      ptt.replace(0, 2, nextTT, randTT(), randTT(), randTT())

      checkProperties(expectedSize = 4)
      p.get.head should be(1)
      pt.get.head should be(TO1)
      pt.get.head should be(TO1)
      ptt.get.head.i should be(nextTT.i)
      ptt.get.head.s should be(nextTT.s)
      ptt.get.head.t.c should be(nextTT.t.c)

      // Insert at the beginning
      nextTT = randTT()
      p.insert(0, 2, 3, 4)
      pt.insert(0, TC1(1), TC2("asd"), TO2)
      ptt.insert(0, nextTT, randTT(), randTT())

      checkProperties(expectedSize = 7)
      p.get.head should be(2)
      pt.get.head should be(TC1(1))
      ptt.get.head.i should be(nextTT.i)
      ptt.get.head.s should be(nextTT.s)
      ptt.get.head.t.c should be(nextTT.t.c)

      // Insert inside
      nextTT = randTT()
      p.insert(3, 5, 3)
      pt.insert(3, TC2("asd"), TO2)
      ptt.insert(3, nextTT, randTT())

      checkProperties(expectedSize = 9)
      p.get(3) should be(5)
      pt.get(3) should be(TC2("asd"))
      ptt.get(3).i should be(nextTT.i)
      ptt.get(3).s should be(nextTT.s)
      ptt.get(3).t.c should be(nextTT.t.c)

      // Insert at the end
      nextTT = randTT()
      p.insert(9, 5, 3)
      pt.insert(9, TC2("asd"), TO2)
      ptt.insert(9, nextTT, randTT())

      checkProperties(expectedSize = 11)
      p.get(9) should be(5)
      pt.get(9) should be(TC2("asd"))
      ptt.get(9).i should be(nextTT.i)
      ptt.get(9).s should be(nextTT.s)
      ptt.get(9).t.c should be(nextTT.t.c)

      // Remove by indexes
      p.remove(1, 8)
      pt.remove(1, 8)
      ptt.remove(1, 8)

      checkProperties(expectedSize = 3)
      p.get(1) should be(5)
      pt.get(1) should be(TC2("asd"))
      ptt.get(1).i should be(nextTT.i)
      ptt.get(1).s should be(nextTT.s)
      ptt.get(1).t.c should be(nextTT.t.c)

      // Remove value
      p.remove(5)
      pt.remove(TC2("asd"))
      ptt.remove(ptt.elemProperties(1).get)

      checkProperties(expectedSize = 2)
      p.get(1) should be(3)
      pt.get(1) should be(TO2)

      // Prepend
      nextTT = randTT()
      p.prepend(2, 3, 4)
      pt.prepend(TC1(1), TC2("asd"), TO2)
      ptt.prepend(nextTT, randTT(), randTT())

      checkProperties(expectedSize = 5)
      p.get.head should be(2)
      pt.get.head should be(TC1(1))
      ptt.get.head.i should be(nextTT.i)
      ptt.get.head.s should be(nextTT.s)
      ptt.get.head.t.c should be(nextTT.t.c)

      // Append
      nextTT = randTT()
      p.append(5, 3)
      pt.append(TC2("asd"), TO2)
      ptt.append(nextTT, randTT())

      checkProperties(expectedSize = 7)
      p.get(5) should be(5)
      pt.get(5) should be(TC2("asd"))
      ptt.get(5).i should be(nextTT.i)
      ptt.get(5).s should be(nextTT.s)
      ptt.get(5).t.c should be(nextTT.t.c)
    }

    "handle null value as empty Seq" in {
      val p = SeqProperty[Int](1,2,3)
      p.set(null)
      p.get.size should be(0)
    }

    "fire value listeners on structure change" in {
      val p = SeqProperty[Int]

      val values = mutable.ArrayBuffer[Seq[Int]]()
      val listener = (s: Seq[Int]) => values += s

      p.listen(listener)

      p.set(Seq(1,2,3))
      values.size should be(1)
      values.last should be(Seq(1, 2, 3))

      p.replace(1, 2, 5, 6)
      values.size should be(2)
      values.last should be(Seq(1, 5, 6))

      p.insert(3, 7, 7)
      values.size should be(3)
      values.last should be(Seq(1, 5, 6, 7, 7))

      p.remove(1, 2)
      values.size should be(4)
      values.last should be(Seq(1, 7, 7))

      p.remove(7)
      values.size should be(5)
      values.last should be(Seq(1, 7))

      p.prepend(1, 2)
      values.size should be(6)
      values.last should be(Seq(1, 2, 1, 7))

      p.append(1, 2)
      values.size should be(7)
      values.last should be(Seq(1, 2, 1, 7, 1, 2))
    }

    "fire value listeners on every child change" in {
      val p = SeqProperty[Int]

      val values = mutable.ArrayBuffer[Seq[Int]]()
      val listener = (s: Seq[Int]) => values += s

      p.listen(listener)

      p.set(Seq(1,2,3))
      values.size should be(1)
      values.last should be(Seq(1, 2, 3))

      p.elemProperties(1).set(5)
      values.size should be(2)
      values.last should be(Seq(1, 5, 3))

      p.elemProperties(0).set(8)
      values.size should be(3)
      values.last should be(Seq(8, 5, 3))

      p.elemProperties(2).set(1)
      values.size should be(4)
      values.last should be(Seq(8, 5, 1))
    }

    "fire structure listeners on structure change" in {
      val p = SeqProperty[Int]

      val patches = mutable.ArrayBuffer[Patch[Property[Int]]]()
      val listener = (s: Patch[Property[Int]]) => patches += s

      p.listenStructure(listener)

      p.set(Seq(1,2,3))
      patches.size should be(1)
      patches.last.idx should be(0)
      patches.last.added.size should be(3)
      patches.last.removed.size should be(0)

      p.replace(1, 2, 5, 6)
      patches.size should be(2)
      patches.last.idx should be(1)
      patches.last.added.size should be(2)
      patches.last.removed.size should be(2)

      p.insert(3, 7, 7)
      patches.size should be(3)
      patches.last.idx should be(3)
      patches.last.added.size should be(2)
      patches.last.removed.size should be(0)

      p.remove(1, 2)
      patches.size should be(4)
      patches.last.idx should be(1)
      patches.last.added.size should be(0)
      patches.last.removed.size should be(2)

      p.remove(7)
      patches.size should be(5)
      patches.last.idx should be(1)
      patches.last.added.size should be(0)
      patches.last.removed.size should be(1)

      p.prepend(1, 2)
      patches.size should be(6)
      patches.last.idx should be(0)
      patches.last.added.size should be(2)
      patches.last.removed.size should be(0)

      p.append(1, 2)
      patches.size should be(7)
      patches.last.idx should be(4)
      patches.last.added.size should be(2)
      patches.last.removed.size should be(0)
    }

    "not fire structure listeners on child change" in {
      val p = SeqProperty[Int]

      val patches = mutable.ArrayBuffer[Patch[Property[Int]]]()
      val listener = (s: Patch[Property[Int]]) => patches += s

      p.listenStructure(listener)

      p.set(Seq(1,2,3))
      patches.size should be(1)
      p.elemProperties(1).set(5)
      patches.size should be(1)
      p.elemProperties(0).set(8)
      patches.size should be(1)
      p.elemProperties(2).set(1)
      patches.size should be(1)
    }

    "transform into Property" in {
      val p = SeqProperty[Int](1, 2, 3)
      val t = p.transform[Int](
        (s: Seq[Int]) => s.sum,
        (i: Int) => (1 to i).toSeq
      )

      p.get should be(Seq(1, 2, 3))
      t.get should be(6)

      t.set(5)

      t.get should be(15) // Notice that `t.set(5)` sets original value to `Seq(1, 2, 3, 4, 5)` and `t.get` is equal 15, not 5!
      p.get should be(Seq(1, 2, 3, 4, 5))
      t.get should be(15)

      p.set(Seq(1, 2))

      p.get should be(Seq(1, 2))
      t.get should be(3)
    }

    "transform into another SeqProperty" in {
      val p = SeqProperty[Int](1, 2, 3)
      val t = p.transform[T](
        (i: Int) => TC1(i),
        (t: T) => t match {
          case TC1(i) => i
          case _: T => 0
        }
      )

      p.get should be(Seq(1, 2, 3))
      t.get should be(Seq(TC1(1), TC1(2), TC1(3)))

      t.prepend(TC1(0))

      p.get should be(Seq(0, 1, 2, 3))
      t.get should be(Seq(TC1(0), TC1(1), TC1(2), TC1(3)))

      p.append(4)

      p.get should be(Seq(0, 1, 2, 3, 4))
      t.get should be(Seq(TC1(0), TC1(1), TC1(2), TC1(3), TC1(4)))

      t.set(Seq(TO1, TO2))

      t.get should be(Seq(TC1(0), TC1(0))) // Again notice that you wont get inserted values, because they were transformed.
      p.get should be(Seq(0, 0))
      t.get should be(Seq(TC1(0), TC1(0)))
    }

    "return immutable sequence from get" in {
      val p = SeqProperty[Int](1, 2, 3)
      p.replace(0, 3, p.get.map(_ + 1):_*)
      p.get should be(Seq(2, 3, 4))
    }

    "return filtered version of sequence" in {
      val p = SeqProperty[Int](1, 2, 3)
      val f = p.filter(_ % 2 == 0)

      f.get should be(Seq(2))
      f.size should be(1)
      f.isEmpty should be(false)
      f.nonEmpty should be(true)

      p.append(4, 5, 6)
      p.prepend(-2, -4, -6)

      f.get should be(Seq(-2, -4, -6, 2, 4, 6))
      f.size should be(6)
      f.isEmpty should be(false)
      f.nonEmpty should be(true)

      p.elemProperties.foreach(el => if (el.get % 2 == 0) el.set(el.get + 1))

      f.get should be(Seq())
      f.size should be(0)
      f.isEmpty should be(true)
      f.nonEmpty should be(false)
    }

    "return filtered version of sequence with ModelProperty" in {
      trait M {
        def x: Int
      }

      case class MI(override val x: Int) extends M

      val p = SeqProperty[M](MI(1), MI(2), MI(3))
      val f = p.filter(_.x % 2 == 0)

      f.get.map(_.x) should be(Seq(2))

      p.append(MI(4), MI(5), MI(6))
      p.prepend(MI(-2), MI(-4), MI(-6))

      f.get.map(_.x) should be(Seq(-2, -4, -6, 2, 4, 6))

      p.elemProperties.foreach(el => if (el.get.x % 2 == 0) el.set(MI(el.get.x + 1)))

      f.get.map(_.x) should be(Seq())
    }

    "provide filtered version of sequence which should fire valid listeners" in {
      val p = SeqProperty[Int](1, 2, 3)
      val f = p.filter(_ % 2 == 0)

      var states = mutable.ArrayBuffer.empty[Seq[Int]]
      var patches = mutable.ArrayBuffer.empty[Patch[ReadableProperty[Int]]]

      f.listen(v => states += v)
      f.listenStructure(p => patches += p)

      p.append(4)

      states.last should be(Seq(2, 4))
      patches.size should be(1)
      patches.last.idx should be(1)
      patches.last.added.map(_.get) should be(Seq(4))
      patches.last.removed.map(_.get) should be(Seq())

      p.append(1, 2, 3, 4)

      states.last should be(Seq(2, 4, 2, 4))
      patches.last.idx should be(2)
      patches.last.added.map(_.get) should be(Seq(2, 4))
      patches.last.removed.map(_.get) should be(Seq())

      p.prepend(1, 2, 3, 4)

      states.last should be(Seq(2, 4, 2, 4, 2, 4))
      patches.last.idx should be(0)
      patches.last.added.map(_.get) should be(Seq(2, 4))
      patches.last.removed.map(_.get) should be(Seq())

      p.replace(2, 10, 0, 0, 0, 1)

      states.last should be(Seq(2, 0, 0, 0))
      patches.last.idx should be(1)
      patches.last.added.map(_.get) should be(Seq(0, 0, 0))
      patches.last.removed.map(_.get) should be(Seq(4, 2, 4, 2, 4))

      p.remove(1, 6)

      states.last should be(Seq())
      patches.last.idx should be(0)
      patches.last.added.map(_.get) should be(Seq())
      patches.last.removed.map(_.get) should be(Seq(2, 0, 0, 0))

      p.set(Seq(1, 2, 3))

      states.last should be(Seq(2))
      patches.last.idx should be(0)
      patches.last.added.map(_.get) should be(Seq(2))
      patches.last.removed.map(_.get) should be(Seq())

      states.clear()
      patches.clear()

      p.elemProperties.foreach(el => el.set(el.get + 1))

      states.size should be(3)
      states(0) should be(Seq(2, 2))
      states(1) should be(Seq(2))
      states(2) should be(Seq(2, 4))
      patches.size should be(3)
      patches(0).idx should be(0)
      patches(0).added.map(_.get) should be(Seq(2))
      patches(0).removed.map(_.get) should be(Seq())
      patches(1).idx should be(1)
      patches(1).added.map(_.get) should be(Seq())
      patches(1).removed.map(_.get) should be(Seq(3)) // now has different value (2 => 3)
      patches(2).idx should be(1)
      patches(2).added.map(_.get) should be(Seq(4))
      patches(2).removed.map(_.get) should be(Seq())

      p.set(Seq(1, 2, 3))

      states.last should be(Seq(2))
      patches.last.idx should be(0)
      patches.last.added.map(_.get) should be(Seq(2))
      patches.last.removed.map(_.get) should be(Seq(2, 4))

      states.clear()
      patches.clear()

      CallbackSequencer.sequence {
        p.elemProperties.foreach(el => el.set(el.get + 1))
        p.elemProperties.foreach(el => el.set(el.get - 1))
        p.elemProperties.foreach(el => el.set(el.get + 1))
      }

      states.size should be(1)
      states(0) should be(Seq(2, 4))
      patches.size should be(3)
      patches(0).idx should be(0)
      patches(0).added.map(_.get) should be(Seq(2))
      patches(0).removed.map(_.get) should be(Seq())
      patches(1).idx should be(1)
      patches(1).added.map(_.get) should be(Seq())
      patches(1).removed.map(_.get) should be(Seq(3)) // now has different value (2 => 3)
      patches(2).idx should be(1)
      patches(2).added.map(_.get) should be(Seq(4))
      patches(2).removed.map(_.get) should be(Seq())

      p.set(Seq(1, 2, 3))

      states.clear()
      patches.clear()

      p.elemProperties.foreach(el => el.set(el.get + 2))

      states.size should be(1)
      states(0) should be(Seq(4))
      patches.size should be(0)

      p.set(Seq(1, 2, 3))

      states.clear()
      patches.clear()

      p.elemProperties.foreach(el => if (el.get % 2 == 0) el.set(el.get + 1))

      states.size should be(1)
      states(0) should be(Seq())
      patches.size should be(1)
      patches(0).idx should be(0)
      patches(0).added.map(_.get) should be(Seq())
      patches(0).removed.map(_.get) should be(Seq(3)) // now has different value (2 => 3)

      p.set(Seq(1, 2, 3))

      states.clear()
      patches.clear()

      p.append(2)
      p.append(4)
      p.append(6)

      states.size should be(3)
      states(0) should be(Seq(2, 2))
      states(1) should be(Seq(2, 2, 4))
      states(2) should be(Seq(2, 2, 4, 6))
      patches.size should be(3)
      patches(0).idx should be(1)
      patches(0).added.map(_.get) should be(Seq(2))
      patches(0).removed.map(_.get) should be(Seq())
      patches(1).idx should be(2)
      patches(1).added.map(_.get) should be(Seq(4))
      patches(1).removed.map(_.get) should be(Seq())
      patches(2).idx should be(3)
      patches(2).added.map(_.get) should be(Seq(6))
      patches(2).removed.map(_.get) should be(Seq())

      p.set(Seq(1, 2, 3))

      states.clear()
      patches.clear()

      CallbackSequencer.sequence {
        p.append(2)
        p.append(4)
        p.append(6)
      }

      states.size should be(1)
      states(0) should be(Seq(2, 2, 4, 6))
      patches.size should be(3)
      patches(0).idx should be(1)
      patches(0).added.map(_.get) should be(Seq(2))
      patches(0).removed.map(_.get) should be(Seq())
      patches(1).idx should be(2)
      patches(1).added.map(_.get) should be(Seq(4))
      patches(1).removed.map(_.get) should be(Seq())
      patches(2).idx should be(3)
      patches(2).added.map(_.get) should be(Seq(6))
      patches(2).removed.map(_.get) should be(Seq())

      f.get should be(Seq(2, 2, 4, 6))
    }

    "be able to modify after transformation" in {
      val numbers = SeqProperty[Int](1, 2, 3)
      val strings = numbers.transform((i: Int) => i.toString, (s: String) => Integer.parseInt(s))

      strings.append("4", "5", "6")
      numbers.get should be(Seq(1, 2, 3, 4, 5, 6))

      strings.remove("4")
      strings.remove("6")
      numbers.get should be(Seq(1, 2, 3, 5))
    }

    "filter transformed property" in {
      val doubles = SeqProperty[Double](1.5, 2.3, 3.7)
      val ints = doubles.transform((d: Double) => d.toInt, (i: Int) => i.toDouble)
      val evens = ints.filter(_ % 2 == 0)

      doubles.get should be(Seq(1.5, 2.3, 3.7))
      ints.get should be(Seq(1, 2, 3))
      evens.get should be(Seq(2))

      doubles.prepend(8.5)

      doubles.get should be(Seq(8.5, 1.5, 2.3, 3.7))
      ints.get should be(Seq(8, 1, 2, 3))
      evens.get should be(Seq(8, 2))

      ints.append(12)

      doubles.get should be(Seq(8.5, 1.5, 2.3, 3.7, 12.0))
      ints.get should be(Seq(8, 1, 2, 3, 12))
      evens.get should be(Seq(8, 2, 12))

      doubles.replace(1, 3, 4.5, 5.5, 6.6)

      doubles.get should be(Seq(8.5, 4.5, 5.5, 6.6, 12.0))
      ints.get should be(Seq(8, 4, 5, 6, 12))
      evens.get should be(Seq(8, 4, 6, 12))

      ints.remove(4)

      doubles.get should be(Seq(8.5, 5.5, 6.6, 12.0))
      ints.get should be(Seq(8, 5, 6, 12))
      evens.get should be(Seq(8, 6, 12))

      doubles.remove(6.6)

      doubles.get should be(Seq(8.5, 5.5, 12.0))
      ints.get should be(Seq(8, 5, 12))
      evens.get should be(Seq(8, 12))

      doubles.append(8.2, 10.3)

      doubles.get should be(Seq(8.5, 5.5, 12.0, 8.2, 10.3))
      ints.get should be(Seq(8, 5, 12, 8, 10))
      evens.get should be(Seq(8, 12, 8, 10))

      ints.remove(5)

      doubles.get should be(Seq(8.5, 12.0, 8.2, 10.3))
      ints.get should be(Seq(8, 12, 8, 10))
      evens.get should be(Seq(8, 12, 8, 10))
    }
  }
}
