package tests

import com.bryghts.ftypes._
import utest._
import utest.ExecutionContext.RunNow

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}

object FTypes extends ValExtensions
                 with BasicFlatteners

import FTypes._

@Async case class Something(inn: async.Int, b: async.Boolean)
@Async case class Other(s: Something)

/**
 * Created by Marc Esquerrà on 23/01/2016.
 */
object SmokeTest  extends TestSuite{
    val tests = TestSuite{
        "Basic Check" - {
            "Simple Comparisson" - {
                val s   = Something(1, false)
                val tmp = s.inn
                val r   = tmp === 1
                r.future.map(r => assert(r))
                val two = Other(s)
                val r2 = two.s.inn === 1
                r2.future.map(r => assert(r))

            }
        }
        "Playing with arrays" - {
            val a = async.Array[async.Int](1, 2, 3)
            val b = a(1)
            val r = b === 2
            r.future.map(r => assert(r))
        }
    }

}
