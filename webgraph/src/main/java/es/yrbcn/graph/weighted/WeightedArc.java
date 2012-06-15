package es.yrbcn.graph.weighted;

public class WeightedArc {
	public int src;

	public int dest;

	public float weight;

	WeightedArc(int src, int dest, float weight) {
		this.src = src;
		this.dest = dest;
		this.weight = weight;
	}
	
	WeightedArc(String line) {
		final String p[] = line.split( "\t" );
		this.src = Integer.parseInt( p[0] );
		this.dest = Integer.parseInt( p[1] );
		this.weight = Float.parseFloat( p[2] );
	}
	
	public String toString() {
		return( "(" + src + "," + dest + "," + weight + ")" );
	}

}
