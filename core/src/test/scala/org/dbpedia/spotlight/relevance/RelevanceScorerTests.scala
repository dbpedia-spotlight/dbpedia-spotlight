package org.dbpedia.spotlight.relevance

import org.junit.Test
import org.junit.Assert._
import org.dbpedia.spotlight.model._
import org.scalatest.mock.MockitoSugar.mock
import org.dbpedia.spotlight.db.model.ContextStore
import org.mockito.Mockito
import java.util.{ArrayList, HashMap}
import org.scalatest.junit.AssertionsForJUnit
import scala.collection.JavaConverters._


/**
 * Tests for the relevance scorer class.
 *
 * @author Thiago Galery (thiago.galery@idioplatform.com)
 * @author David Przybilla (david.przybilla@idioplatform.com)
 */
class RelevanceScorerTest extends AssertionsForJUnit {

  /** Tests whether the context vectors are returned with the appropriate counts */
  @Test
  def testGetContextVectors{

    // Let's mock up some dbpedia resources.
    val barack = new DBpediaResource("http://dbpedia.org/resource/Barack_Obama",
      567,
      0.9,
      List[OntologyType]()
    )
    val marilyn = new DBpediaResource("http://dbpedia.org/resource/Marilyn_Monroe",
      675,
      0.7,
      List[OntologyType]()
    )
    // Add entities to a resource list
    val resourceList =  List[DBpediaResource](marilyn, barack)

    // Let's mock up some token type-counts associations.
    val p = new HashMap[TokenType, Int]
    p.put(mock[TokenType],4)
    val s = new HashMap[TokenType, Int]
    s.put(mock[TokenType],5)

    // Mock the context store
    val contextStore = mock[ContextStore]
    // Monkeypatch the behaviour of the context store
    Mockito.when(contextStore.getContextCounts(barack)).thenReturn(p)
    Mockito.when(contextStore.getContextCounts(marilyn)).thenReturn(s)
    // Create an instance of the scorer
    val relevance = new ContextRelevanceScore()
    val scorer = new RelevanceScorer(contextStore, relevance)
    // Run the method on the mocked up data
    val result = scorer.getContextVectors(resourceList)
    // Expect mocked up objects to appear as keys in the result
    assertTrue(result.contains(barack))
    assertTrue(result.contains(marilyn))
    // Expect Barack to have a TokenType with 4 occurrences, but not 5
    assertTrue(result.get(barack).get.containsValue(4))
    assertFalse(result.get(barack).get.containsValue(5))
    // Expect Marilyn to have a TokenType with 5 occurrences, but not 4
    assertFalse(result.get(marilyn).get.containsValue(4))
    assertTrue(result.get(marilyn).get.containsValue(5))

  }
  /** Tests whether the text vectors are cleaned correctly */
  @Test
  def testProcessTextVector{

    val token1 = new Token("Simon", 1, mock[TokenType])
    val token2 = new Token("of", 6, TokenType.STOPWORD)
    val token3 = new Token("Greece", 10, mock[TokenType])

    // Add entities to a token list
    val tokenList =  List[Token](token1, token2, token3)
    // Mock the context store
    val contextStore = mock[ContextStore]
    // Create an instance of the scorer
    val relevance = new ContextRelevanceScore()
    val scorer = new RelevanceScorer(contextStore, relevance)
    // Run the method on the mocked up data
    val result = scorer.processTextVector(tokenList)
    // Expect only two token types in the result, since one of them is a stop word
    assertTrue(result.size.equals(2))

  }

  /** Tests the calculation of the relevances is done properly */
  @Test
  def testCalculateRelevances{

    //Mock the data
    val testString = "This is a piece on Barack Obama and Marilyn Monroe . They are Americans ."
    val pseudoTokenList = testString.split(" ")
    var tokenList = new ArrayList[Token]()

    for (x <- pseudoTokenList){
      var tok = new Token(x, testString.indexOf(x), mock[TokenType])
      tokenList.add(tok)

    }

    val testText = new Text(testString)
    //Mock the 'token' feature of the text object
    testText.setFeature(new Feature("tokens", tokenList.asScala.toList))

    // Let's mock up some dbpedia resources.
    val barack = new DBpediaResource("http://dbpedia.org/resource/Barack_Obama",
      567,
      0.9,
      List[OntologyType]()
    )
    val marilyn = new DBpediaResource("http://dbpedia.org/resource/Marilyn_Monroe",
      675,
      0.7,
      List[OntologyType]()
    )
    var barackOcc = new DBpediaResourceOccurrence("Barack_Obama", barack, new SurfaceForm("Barack Obama"), testText, 19)

    var marilynOcc = new DBpediaResourceOccurrence("Marilyn_Monroe", marilyn, new SurfaceForm("Marilyn Monroe"), testText, 37)

    var occList = new ArrayList[DBpediaResourceOccurrence]()
    occList.add(barackOcc)
    occList.add(marilynOcc)

    // Mock the context store
    val contextStore = mock[ContextStore]
    // Create an instance of the scorer
    val relevance = new ContextRelevanceScore()
    val scorer = new RelevanceScorer(contextStore, relevance)
    // Run the method on the mocked up data
    val result = scorer.calculateRelevances(occList, testText)
    // Expect both DBpedia resources to be set as keys
    assertTrue(result.containsKey(barack))
    assertTrue(result.containsKey(marilyn))
    // Note: Since the prior, context and text are identical,
    // the relevance of each enetity is the same.
    assertEquals(result.get(barack),result.get(marilyn))

  }
}