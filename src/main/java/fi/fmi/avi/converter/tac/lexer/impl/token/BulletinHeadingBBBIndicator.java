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
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.MeteorologicalBulletin;

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
            if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                BulletinHeading heading = ((MeteorologicalBulletin) msg).getHeading();
                if (heading != null) {
                    Optional<Integer> augNumber = heading.getBulletinAugmentationNumber();
                    if (augNumber.isPresent()) {
                        if (heading.getType() == BulletinHeading.Type.NORMAL) {
                            throw new SerializingException("Bulletin contains augmentation number, but the type is " + BulletinHeading.Type.NORMAL);
                        }
                        int seqNumber = augNumber.get().intValue();
                        if (seqNumber < 1 || seqNumber > ('Z' - 'A' + 1)) {
                            throw new SerializingException(
                                    "Illegal bulletin augmentation number '" + augNumber.get() + "', the value must be between 1 and  " + ('Z' - 'A' + 1));
                        }
                        seqNumber = 'A' + seqNumber - 1;
                        return Optional.of(createLexeme(heading.getType().getPrefix() + String.valueOf(Character.toChars(seqNumber)),
                                Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR));
                    }
                }
            }
            return retval;
        }
    }
}
