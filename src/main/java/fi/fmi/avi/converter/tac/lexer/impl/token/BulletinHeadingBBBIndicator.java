package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.MeteorologicalBulletin;

public class BulletinHeadingBBBIndicator extends RegexMatchingLexemeVisitor {

    public BulletinHeadingBBBIndicator(final Priority prio) {
        super("^(?<bbb>(?:RR|AA|CC)[A-Z])$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS.equals(token.getFirst().getIdentityIfAcceptable())
                && token.hasPrevious() && token.getPrevious().getIdentityIfAcceptable() != null
                && LexemeIdentity.ISSUE_TIME.equals(token.getPrevious().getIdentityIfAcceptable())) {
            token.identify(LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("bbb"));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            final Optional<Lexeme> retval = Optional.empty();
            if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                final BulletinHeading heading = ((MeteorologicalBulletin) msg).getHeading();
                if (heading != null) {
                    final Optional<Integer> augNumber = heading.getBulletinAugmentationNumber();
                    if (augNumber.isPresent()) {
                        if (heading.getType() == BulletinHeading.Type.NORMAL) {
                            throw new SerializingException("Bulletin contains augmentation number, but the type is " + BulletinHeading.Type.NORMAL);
                        }
                        final int seqNumber = Character.codePointAt("A", 0) + augNumber.get().intValue() - 1;
                        //Using Character.codePointAt here is a bit overdo here since we know that we are always operating with single char ASCII codes
                        if (seqNumber < Character.codePointAt("A", 0) || seqNumber > Character.codePointAt("Z", 0)) {
                            throw new SerializingException(
                                    "Illegal bulletin augmentation number '" + augNumber.get() + "', the value must be between 1 and  " + ('Z' - 'A' + 1));
                        }
                        return Optional.of(createLexeme(heading.getType().getPrefix() + String.valueOf(Character.toChars(seqNumber)),
                                LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR));
                    }
                }
            }
            return retval;
        }
    }
}
