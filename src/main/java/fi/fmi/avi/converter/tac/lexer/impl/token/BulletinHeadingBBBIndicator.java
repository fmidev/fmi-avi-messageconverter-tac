package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.TAFBulletinHeading;

public class BulletinHeadingBBBIndicator extends RegexMatchingLexemeVisitor {

    public BulletinHeadingBBBIndicator(final Priority prio) {
        super("^(?<type>RR|AA|CC)(?<seqno>[A-Z])$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        //TODO: identification and property parsing
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (TAFBulletin.class.isAssignableFrom(clz)) {
                TAFBulletinHeading heading = ((TAFBulletin) msg).getHeading();
                if (heading != null) {
                    Optional<Integer> augNumber = heading.getBulletinAugmentationNumber();
                    if (augNumber.isPresent()) {
                        int seqNumber = augNumber.get();
                        if (seqNumber < 1 || seqNumber > ('Z' - 'A' + 2)) {
                            throw new SerializingException(
                                    "Illegal bulletin augmentation number '" + heading.getBulletinAugmentationNumber() + "', the value must be between 1 and  "
                                            + ('Z' - 'A' + 1));
                        }
                        seqNumber = 'A' + seqNumber - 1;
                        StringBuilder sb = new StringBuilder();
                        if (heading.isContainingDelayedMessages()) {
                            sb.append("RR");
                        } else if (heading.isContainingAmendedMessages()) {
                            sb.append("AA");
                        } else if (heading.isContainingCorrectedMessages()) {
                            sb.append("CC");
                        }
                        sb.append(Character.toChars(seqNumber));
                        return Optional.of(createLexeme(sb.toString(), Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR));
                    }
                }
            }
            return retval;
        }
    }
}
