package org.dbpedia.spotlight.model

class Surrogate(val surfaceForm: SurfaceForm, val resource: DBpediaResource)
{
    def equals(that : Surrogate) : Boolean =
    {
        surfaceForm.equals(that.surfaceForm) && resource.equals(that.resource)
    }

    override def toString = "Surrogate["+surfaceForm.name+","+resource.uri+"]"
}


object Surrogate
{
    def apply(surfaceForm : SurfaceForm, resource: DBpediaResource) = {
        new Surrogate(surfaceForm, resource)
    }
}