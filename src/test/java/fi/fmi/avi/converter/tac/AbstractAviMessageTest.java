package fi.fmi.avi.converter.tac;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.reflectionassert.ReflectionComparator;
import org.unitils.reflectionassert.ReflectionComparatorFactory;
import org.unitils.reflectionassert.comparator.Comparator;
import org.unitils.reflectionassert.difference.Difference;
import org.unitils.reflectionassert.report.impl.DefaultDifferenceReport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public abstract class AbstractAviMessageTest<S, T> {

	private static final double FLOAT_EQUIVALENCE_THRESHOLD = 0.0000000001d;
	
    @Autowired
    private AviMessageLexer lexer;

    @Autowired
	@Qualifier("tacTokenizer")
	private AviMessageTACTokenizer tokenizer;

    @Autowired
    private AviMessageConverter converter;

	public abstract S getMessage();

	public abstract String getTokenizedMessagePrefix();

	public abstract String getJsonFilename();

	public abstract ConversionSpecification<S, T> getParsingSpecification();
	
	public abstract ConversionSpecification<T, S> getSerializationSpecification();

	public abstract Class<? extends AviationWeatherMessage> getTokenizerImplmentationClass();

	/**
	 * The tokenized POJO needs to be equal to this message. By default it returns what getMessage() returns.
	 * Override this if the reconstructed message is to be expected to be a bit different from the original.
	 *  
	 * @see #testTokenizer()
	 * @see #getMessage()
	 */
	public Optional<S> getCanonicalMessage() {
		return Optional.of(getMessage());
	}
	
	public ConversionHints getLexerParsingHints() {
		return new ConversionHints();
	}

	public ConversionHints getParserConversionHints() {
		return new ConversionHints();
	}
    
    public abstract Identity[] getLexerTokenSequenceIdentity();

	public ConversionHints getTokenizerParsingHints() {
		ConversionHints hints = new ConversionHints(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_LONG);
		return hints;
    }
    
	@Test
	public void testLexer() {
		Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
		
		LexemeSequence result = lexer.lexMessage((String) getMessage(), getLexerParsingHints());
		assertTokenSequenceIdentityMatch(result, getLexerTokenSequenceIdentity());
	}
	
	@Test
	public void testTokenizer() throws SerializingException, IOException {
		Assume.assumeTrue(String.class.isAssignableFrom(getSerializationSpecification().getOutputClass()));
		Assume.assumeTrue(getCanonicalMessage().isPresent());
        String expectedMessage = getTokenizedMessagePrefix() + getCanonicalMessage().get();
        assertTokenSequenceMatch(expectedMessage, getJsonFilename(), getTokenizerParsingHints());
	}

	public ConversionResult.Status getExpectedParsingStatus() {
		return ConversionResult.Status.SUCCESS;
	}
	
	public ConversionResult.Status getExpectedSerializationStatus() {
		return ConversionResult.Status.SUCCESS;
	}

	// Override when necessary
	public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
		assertEquals("No parsing issues expected", 0, conversionIssues.size());
	}
	
	// Override when necessary
	public void assertSerializationIssues(List<ConversionIssue> conversionIssues) {
		assertEquals("No serialization issues expected", 0, conversionIssues
				.size());
	}
	
	@Test
	public void testStringToPOJOParser() throws IOException {
		ConversionSpecification<S, T> spec = getParsingSpecification();
		Assume.assumeTrue(String.class.isAssignableFrom(spec.getInputClass()) && AviationWeatherMessage.class.isAssignableFrom(spec.getOutputClass()));
		ConversionResult<? extends AviationWeatherMessage> result = (ConversionResult<? extends AviationWeatherMessage>) converter.convertMessage(getMessage(), spec, getParserConversionHints());
		assertEquals("Parsing was not successful: " + result.getConversionIssues(), getExpectedParsingStatus(), result.getStatus());
		assertParsingIssues(result.getConversionIssues());

		if (result.getConvertedMessage().isPresent()) {
			assertAviationWeatherMessageEquals(readFromJSON(getJsonFilename()), result.getConvertedMessage().get());
		}
	}
	
	@Test
	public void testPOJOToStringSerialiazer() throws IOException {
		ConversionSpecification<T, S> spec = getSerializationSpecification();
		Assume.assumeTrue(AviationWeatherMessage.class.isAssignableFrom(spec.getInputClass()) && String.class.isAssignableFrom(spec.getOutputClass()));
		T input = (T)readFromJSON(getJsonFilename());
		ConversionResult<String> result = (ConversionResult<String>) converter.convertMessage(input, spec, getTokenizerParsingHints());
		assertEquals("Serialization was not successful: " + result.getConversionIssues(), getExpectedSerializationStatus(), result.getStatus());
		assertSerializationIssues(result.getConversionIssues());
		if (result.getConvertedMessage().isPresent() && getCanonicalMessage().isPresent()) {
            String expectedMessage = getTokenizedMessagePrefix() + getCanonicalMessage().get();
            assertEquals(expectedMessage, result.getConvertedMessage().get());
		} else if (!getCanonicalMessage().isPresent()) {
		    assertFalse(result.getConvertedMessage().isPresent());
        } else {
		    fail("Converted message is not present when one is expected");
        }
	}

    public static Identity[] spacify(Identity[] input) {
		List<Identity> retval = new ArrayList<>();
		if (input != null) {
			for (int i=0; i < input.length; i++) {
				retval.add(input[i]);
			    if ((i < input.length - 1) && (Identity.END_TOKEN != input[i + 1])) {
                    retval.add(Identity.WHITE_SPACE);
                }
			}
		}
        return retval.toArray(new Identity[0]);
    }

    public static void assertTokenSequenceIdentityMatch(LexemeSequence result, Lexeme.Identity... identities) {
		List<Lexeme> lexemes = result.getLexemes();
        //assertTrue("Token sequence size does not match", identities.length == lexemes.size());
		for (int i = 0; i < identities.length; i++) {
			assertEquals("Mismatch at index " + i, identities[i], lexemes.get(i).getIdentityIfAcceptable());
		}
	}

	protected void assertTokenSequenceMatch(final String expected, final String fileName, final ConversionHints hints)
			throws IOException, SerializingException {
		LexemeSequence seq = tokenizer.tokenizeMessage(readFromJSON(fileName), hints);
		assertNotNull("Null sequence was produced", seq);
		assertEquals("expected '"+expected+"' does not match with actual'"+seq.getTAC()+"'", expected, seq.getTAC());
	}

	protected AviationWeatherMessage readFromJSON(String fileName) throws IOException {
		AviationWeatherMessage retval = null;
		ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        InputStream is = AbstractAviMessageTest.class.getResourceAsStream(fileName);
		if (is != null) {
            Class<? extends AviationWeatherMessage> clz = getTokenizerImplmentationClass();
            if (SPECI.class.isAssignableFrom(clz)) {
                retval = om.readValue(is, METARImpl.class);
                retval = METARImpl.Builder.from((METAR) retval).buildAsSPECI();
            } else {
                retval = om.readValue(is, clz);
            }
        } else {
			throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
		}
		return retval;
    }
    
    protected static void assertAviationWeatherMessageEquals(AviationWeatherMessage expected, AviationWeatherMessage actual) {
    	Difference diff = deepCompareObjects(expected, actual);
    	if (diff != null) {
            StringBuilder failureMessage = new StringBuilder();
            failureMessage.append("AviationWeatherMessage objects are not equivalent\n");
            failureMessage.append(new DefaultDifferenceReport().createReport(diff));
            fail(failureMessage.toString());
    	}
    }
    
    protected static Difference deepCompareObjects(Object expected, Object actual) {
    	
    	// Use anonymous class to call protected member function
    	LinkedList<Comparator> comparatorChain = (new ReflectionComparatorFactory() {
	    		LinkedList<Comparator> createBaseComparators() {
	    			return new LinkedList<Comparator>(getComparatorChain(Collections.emptySet()));
	    		}
	    	}).createBaseComparators();

        // Add lenient collection comparator ([] == null) as first-in-chain
    	comparatorChain.addFirst(new Comparator() {
			
			@Override
			public Difference compare(Object left, Object right, boolean onlyFirstDifference,
					ReflectionComparator reflectionComparator) {
				Collection<?> coll = (Collection<?>)left;
				if (coll == null) {
					coll = (Collection<?>)right;
				}
				
				if (coll.size() == 0) {
					return null;
				}
				
				return new Difference("Null list does not match a non-empty list", left, right);
			}
			
			@Override
			public boolean canCompare(Object left, Object right) {
				return
					(left == null && right instanceof Collection<?>) || 
					(right == null && left instanceof Collection<?>);
			}
		});
    	
    	// Add double comparator with specified accuracy as first-in-chain
    	comparatorChain.addFirst(new Comparator() {
    		@Override
    		public Difference compare(Object left, Object right, boolean onlyFirstDifference,
    				ReflectionComparator reflectionComparator) {
    			double diff = Math.abs( ((Double)left) - ((Double)right));
    			if (diff >= FLOAT_EQUIVALENCE_THRESHOLD) {
    				return new Difference("Floating point values differ more than set threshold", left, right);
    			}
    			
    			return null;
    		}
    		
    		@Override
    		public boolean canCompare(Object left, Object right) {
    			return left instanceof Double && right instanceof Double;
    		}
    	
    	});
    	
    	
    	ReflectionComparator reflectionComparator = new ReflectionComparator(comparatorChain);
    	return reflectionComparator.getDifference(expected, actual);
    }
}
