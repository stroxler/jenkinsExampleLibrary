import com.stroxler.Bar

import java.util.Map
import java.util.HashMap


def doThing(thing, f) {
    println("starting ${thing}")
    f()
    println("ending ${thing}")
}


doThing "myfunction", {
    x = new HashMap<String, String>()
    x.put("woah", "cool")
    println(x.containsKey("woah"))
    println(x.get("oops"))
}
