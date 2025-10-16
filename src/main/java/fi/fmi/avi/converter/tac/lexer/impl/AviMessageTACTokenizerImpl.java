package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.airmet.AIRMETBulletinTACSerializer;
import fi.fmi.avi.converter.tac.airmet.AIRMETTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinTACSerializer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.metar.METARTACSerializer;
import fi.fmi.avi.converter.tac.metar.SPECITACSerializer;
import fi.fmi.avi.converter.tac.sigmet.SIGMETBulletinTACSerializer;
import fi.fmi.avi.converter.tac.sigmet.SIGMETTACSerializer;
import fi.fmi.avi.converter.tac.swx.SWXTACSerializer;
import fi.fmi.avi.converter.tac.taf.TAFBulletinTACSerializer;
import fi.fmi.avi.converter.tac.taf.TAFTACSerializer;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AIRMETBulletin;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

public class AviMessageTACTokenizerImpl implements AviMessageTACTokenizer {
    private METARTACSerializer metarSerializer;
    private SPECITACSerializer speciSerializer;
    private TAFTACSerializer tafSerializer;
    private TAFBulletinTACSerializer tafBulletinSerializer;
    private SIGMETBulletinTACSerializer sigmetBulletinSerializer;
    private AIRMETBulletinTACSerializer airmetBulletinSerializer;
    private GenericMeteorologicalBulletinTACSerializer genericBulletinSerializer;
    private SWXTACSerializer swxTACSerializer;
    private SIGMETTACSerializer sigmetTACSerializer;
    private AIRMETTACSerializer airmetTACSerializer;

    public AviMessageTACTokenizerImpl() {
    }

    public void setMETARSerializer(final METARTACSerializer serializer) {
        this.metarSerializer = serializer;
    }

    public void setSPECISerializer(final SPECITACSerializer serializer) {
        this.speciSerializer = serializer;
    }

    public void setTAFSerializer(final TAFTACSerializer serializer) {
        this.tafSerializer = serializer;
    }

    public void setTAFBulletinSerializer(final TAFBulletinTACSerializer serializer) {
        this.tafBulletinSerializer = serializer;
    }

    public void setSIGMETBulletinSerializer(final SIGMETBulletinTACSerializer serializer) {
        this.sigmetBulletinSerializer = serializer;
    }

    public void setAIRMETBulletinSerializer(final AIRMETBulletinTACSerializer serializer) {
        this.airmetBulletinSerializer = serializer;
    }


    public void setGenericBulletinSerializer(final GenericMeteorologicalBulletinTACSerializer serializer) {
        this.genericBulletinSerializer = serializer;
    }

    public void setSWXTacSerializer(final SWXTACSerializer serializer) {
        this.swxTACSerializer = serializer;
    }

    public void setSIGMETTacSerializer(final SIGMETTACSerializer serializer) {
        this.sigmetTACSerializer = serializer;
    }

    public void setAIRMETTacSerializer(final AIRMETTACSerializer serializer) {
        this.airmetTACSerializer = serializer;
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return this.tokenizeMessage(msg, null);
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (msg instanceof SPECI && this.speciSerializer != null) {
            return this.speciSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof METAR && this.metarSerializer != null) {
            return this.metarSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof TAF && this.tafSerializer != null) {
            return this.tafSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof TAFBulletin && this.tafBulletinSerializer != null) {
            return this.tafBulletinSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof SIGMETBulletin && this.sigmetBulletinSerializer != null) {
            return this.sigmetBulletinSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof AIRMETBulletin && this.airmetBulletinSerializer != null) {
            return this.airmetBulletinSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof GenericMeteorologicalBulletin && this.genericBulletinSerializer != null) {
            return this.genericBulletinSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof SpaceWeatherAdvisoryAmd79 && this.swxTACSerializer != null) {
            return this.swxTACSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof SIGMET && this.sigmetTACSerializer != null) {
            return this.sigmetTACSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof AIRMET && this.airmetTACSerializer != null) {
            return this.airmetTACSerializer.tokenizeMessage(msg, hints);
        } else
        throw new IllegalArgumentException("Do not know how to tokenize message of type " + msg.getClass().getCanonicalName());
    }

}
