package example.rsocket.domain

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("TreeSpecies")
class TreeSpecies
 {
     var id: Long? = null
     var leaf: String? = null

     constructor(id: Long, leaf: String) {
         this.id = id
         this.leaf = leaf
     }

     constructor() { }
}