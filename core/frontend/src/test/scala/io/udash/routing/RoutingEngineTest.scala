package io.udash.routing

import io.udash._
import io.udash.testing._

import scala.collection.mutable.ListBuffer

class RoutingEngineTest extends UdashFrontendTest with TestRouting {

  "RoutingEngine" should {
    "render valid views on url change" in {
      val rootView = new TestView
      val objectView = new TestView
      val nextObjectView = new TestView
      val classView = new TestView
      val class2View = new TestView
      val errorView = new TestView
      val state2VP: Map[TestState, ViewPresenter[_ <: TestState]] = Map(
        RootState -> new DefaultViewPresenterFactory[RootState.type](() => rootView) {},
        ObjectState -> new DefaultViewPresenterFactory[ObjectState.type](() => objectView) {},
        NextObjectState -> new DefaultViewPresenterFactory[NextObjectState.type](() => nextObjectView) {},
        ClassState("abc", 1) -> new DefaultViewPresenterFactory[ClassState](() => classView) {},
        ClassState("abcd", 234) -> new DefaultViewPresenterFactory[ClassState](() => class2View) {},
        ErrorState -> new DefaultViewPresenterFactory[ErrorState.type](() => errorView) {}
      )

      initTestRoutingEngine(state2vp = state2VP)

      routingEngine.handleUrl(Url("/"))

      renderer.views.size should be(2)
      renderer.views(0) should be(rootView)
      renderer.views(1) should be(objectView)
      renderer.lastSubPathToLeave should be(Nil)
      renderer.lastPathToAdd.size should be(2)

      routingEngine.handleUrl(Url("/next"))

      renderer.views.size should be(3)
      renderer.views(0) should be(rootView)
      renderer.views(1) should be(objectView)
      renderer.views(2) should be(nextObjectView)
      renderer.lastSubPathToLeave.size should be(2)
      renderer.lastPathToAdd should be(nextObjectView :: Nil)

      routingEngine.handleUrl(Url("/"))

      renderer.views.size should be(2)
      renderer.views(0) should be(rootView)
      renderer.views(1) should be(objectView)
      renderer.lastSubPathToLeave.size should be(2)
      renderer.lastPathToAdd.size should be(0)

      routingEngine.handleUrl(Url("/abc/1"))

      renderer.views.size should be(2)
      renderer.views(0) should be(rootView)
      renderer.views(1) should be(classView)
      renderer.lastSubPathToLeave.size should be(1)
      renderer.lastPathToAdd.size should be(1)

      routingEngine.handleUrl(Url("/abcd/234"))

      renderer.views.size should be(2)
      renderer.views(0) should be(rootView)
      renderer.views(1) should be(class2View)
      renderer.lastSubPathToLeave.size should be(1)
      renderer.lastPathToAdd.size should be(1)

      routingEngine.handleUrl(Url("/next"))

      renderer.views.size should be(3)
      renderer.views(0) should be(rootView)
      renderer.views(1) should be(objectView)
      renderer.views(2) should be(nextObjectView)
      renderer.lastSubPathToLeave.size should be(1)
      renderer.lastPathToAdd should be(objectView :: nextObjectView :: Nil)
    }

    "fire state change callbacks" in {
      initTestRoutingEngine()

      var calls = 0
      var lastCallbackEvent: StateChangeEvent[TestState] = null
      routingEngine.onStateChange(ev => {
        lastCallbackEvent = ev
        calls += 1
      })

      routingEngine.handleUrl(Url("/"))

      calls should be(1)
      lastCallbackEvent.oldState should be(null)
      lastCallbackEvent.currentState should be(ObjectState)

      routingEngine.handleUrl(Url("/next"))

      calls should be(2)
      lastCallbackEvent.oldState should be(ObjectState)
      lastCallbackEvent.currentState should be(NextObjectState)

      routingEngine.handleUrl(Url("/"))

      calls should be(3)
      lastCallbackEvent.oldState should be(NextObjectState)
      lastCallbackEvent.currentState should be(ObjectState)

      routingEngine.handleUrl(Url("/"))

      calls should be(3)
      lastCallbackEvent.oldState should be(NextObjectState)
      lastCallbackEvent.currentState should be(ObjectState)

      routingEngine.handleUrl(Url("/abc/1"))

      calls should be(4)
      lastCallbackEvent.oldState should be(ObjectState)
      lastCallbackEvent.currentState should be(ClassState("abc", 1))

      routingEngine.handleUrl(Url("/abc/1"))

      calls should be(4)
      lastCallbackEvent.oldState should be(ObjectState)
      lastCallbackEvent.currentState should be(ClassState("abc", 1))

      routingEngine.handleUrl(Url("/abcd/234"))

      calls should be(5)
      lastCallbackEvent.oldState should be(ClassState("abc", 1))
      lastCallbackEvent.currentState should be(ClassState("abcd", 234))

      routingEngine.handleUrl(Url("/next"))

      calls should be(6)
      lastCallbackEvent.oldState should be(ClassState("abcd", 234))
      lastCallbackEvent.currentState should be(NextObjectState)

      routingEngine.handleUrl(Url("/next"))

      calls should be(6)
      lastCallbackEvent.oldState should be(ClassState("abcd", 234))
      lastCallbackEvent.currentState should be(NextObjectState)
    }

    "return valid current app state" in {
      initTestRoutingEngine()

      routingEngine.handleUrl(Url("/"))
      routingEngine.currentState should be(ObjectState)

      routingEngine.handleUrl(Url("/next"))
      routingEngine.currentState should be(NextObjectState)

      routingEngine.handleUrl(Url("/"))
      routingEngine.currentState should be(ObjectState)

      routingEngine.handleUrl(Url("/abc/1"))
      routingEngine.currentState should be(ClassState("abc", 1))

      routingEngine.handleUrl(Url("/abcd/234"))
      routingEngine.currentState should be(ClassState("abcd", 234))

      routingEngine.handleUrl(Url("/next"))
      routingEngine.currentState should be(NextObjectState)
    }
  }
}
