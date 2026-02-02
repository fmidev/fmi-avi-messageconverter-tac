package fi.fmi.avi.converter.tac.bulletin;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT1;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.util.GTSExchangeFileInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericMeteorologicalBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    @Autowired
    private AviMessageLexer lexer;

    @Test
    public void testBulletinLexing() {
        final LexemeSequence seq = lexer.lexMessage("FTFI33 EFPP 020500\n" //
                + "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n" //
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n" //
                + "BKN010 SCT030CB=");
        Lexeme l = seq.getFirstLexeme();
        assertEquals(LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(LexemeIdentity.ISSUE_TIME, l.getIdentityIfAcceptable());
        while (l.hasNext()) {
            l = l.getNext();
        }
        assertEquals(LexemeIdentity.END_TOKEN, l.getIdentityIfAcceptable());
    }

    @Test
    public void testAmendmentBulletinLexing() {
        final LexemeSequence seq = lexer.lexMessage("FTFI33 EFPP 020500 AAA\n" //
                + "TAF AMD EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n" //
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n" //
                + "BKN010 SCT030CB=");
        Lexeme l = seq.getFirstLexeme();
        assertEquals(LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(LexemeIdentity.ISSUE_TIME, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR, l.getIdentityIfAcceptable());
        while (l.hasNext()) {
            l = l.getNext();
        }
        assertEquals(LexemeIdentity.END_TOKEN, l.getIdentityIfAcceptable());
    }

    @Test
    public void testTAFBulletinParsing() {
        final BulletinHeading heading = BulletinHeadingImpl.builder()//
                .setDataTypeDesignatorT1ForTAC(DataTypeDesignatorT1.FORECASTS)
                .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)
                .setBulletinNumber(33)
                .setGeographicalDesignator("FI")
                .setLocationIndicator("EFPP")
                .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))
                .setType(BulletinHeading.Type.NORMAL)
                .build();
        final GTSExchangeFileInfo info = GTSExchangeFileInfo.builder().setHeading(heading)
                .setFileType(GTSExchangeFileInfo.GTSExchangeFileType.TEXT)
                .setMetadataFile(false)
                .setPFlag(GTSExchangeFileInfo.GTSExchangePFlag.A)
                .setTimeStampDay(2)
                .setTimeStampHour(5)
                .setTimeStampMinute(0)
                .build();
        final ConversionHints hints = new ConversionHints(ConversionHints.KEY_BULLETIN_ID, info.toGTSExchangeFilename());
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("FTFI33 EFPP 020500\n" //
                + "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n" //
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n" //
                + "BKN010 SCT030CB=\n" //
                + "FAK EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n" //
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n" //
                + "BKN010 SCT030CB=EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n" //
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n" //
                + "BKN010 SCT030CB=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, hints);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(heading, bulletin.get().getHeading());
        assertEquals(3, bulletin.get().getMessages().size());
        GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);
        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.TAF, msg.getMessageType().get());
        assertTrue(msg.getTranslatedBulletinID().isPresent());
        assertEquals(info.toGTSExchangeFilename(), msg.getTranslatedBulletinID().get());

        msg = bulletin.get().getMessages().get(1);
        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.TAF, msg.getMessageType().get());
        assertTrue(msg.getTranslatedBulletinID().isPresent());
        assertEquals(info.toGTSExchangeFilename(), msg.getTranslatedBulletinID().get());

        msg = bulletin.get().getMessages().get(2);
        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.TAF, msg.getMessageType().get());
        assertTrue(msg.getTranslatedBulletinID().isPresent());
        assertEquals(info.toGTSExchangeFilename(), msg.getTranslatedBulletinID().get());

    }

    @Test
    public void testMETARBulletinParsing() {
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("SAFI33 EFPP 020500\n" //
                + "METAR COR EFUT 111115Z 18004KT 150V240 1500 0500N R04R/1500N R15/M0050D R22L/1200N R04L/P1000U SN VV006 M08/M10\n"
                + "Q1023 RESN TEMPO 0900=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.SURFACE_DATA, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.SurfaceDataTypeDesignatorT2.SD_AVIATION_ROUTINE_REPORTS, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        final GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);
        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.METAR, msg.getMessageType().get());
        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndicators =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EFUT");
        assertEquals(expectedIndicators, msg.getLocationIndicators());
        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(11, 11, 15, ZoneId.of("Z"))), msg.getIssueTime().get());
    }

    @Test
    public void testLowWindBulletinParsing() {
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("FTFI33 EFPP 020500\n" //
                        + "LOW WIND EFHK 040820Z  1000FT     2000FT     FL050      FL100 200/20     200/25     210/30     210/20=",
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.FORECASTS, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        final GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);
        assertTrue(msg.getMessageType().isPresent());
        assertEquals("LOW_WIND", msg.getMessageType().get().name());
        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndicators =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EFHK");
        assertEquals(expectedIndicators, msg.getLocationIndicators());
        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(4, 8, 20, ZoneId.of("Z"))), msg.getIssueTime().get());
    }

    @Test
    public void testWXWRNGBulletinParsing() {
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("WSFI31 EFHK 310600\n" //
                + "WX WRNG EFHK 310600Z NIL=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.WARNINGS, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_SIGMET, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        final GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals("WX_WARNING", msg.getMessageType().get().name());

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndicators =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EFHK");
        assertEquals(expectedIndicators, msg.getLocationIndicators());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(31, 6, 0, ZoneId.of("Z"))), msg.getIssueTime().get());
    }

    @Test
    public void testWXREPBulletinParsing() {
        ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("UAFI31 EFHK 310555\n" //
                + "WXREP T01 REP 0555 N6520 E02522 FBL TURB FL230=\n" //
                + "T01 REP 0555 N6520 E02522 FBL TURB FL230=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.WITH_WARNINGS, result.getStatus());
        Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.UPPER_AIR_DATA, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_AIRCRAFT_REPORT_CODAR_AIREP, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(2, bulletin.get().getMessages().size());
        GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals("WXREP", msg.getMessageType().get().name());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(-1, 5, 55, ZoneId.of("Z"))), msg.getIssueTime().get());

        msg = bulletin.get().getMessages().get(1);
        assertFalse(msg.getMessageType().isPresent());

        final ConversionHints hints = new ConversionHints(ConversionHints.KEY_CONTAINED_MESSAGE_TYPE, new MessageType("WXREP"));
        result = this.converter.convertMessage("UAFI31 EFHK 310555\n" //
                + "WXREP T01 REP 0555 N6520 E02522 FBL TURB FL230=\n" //
                + "T01 REP 0555 N6520 E02522 FBL TURB FL230=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, hints);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.UPPER_AIR_DATA, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_AIRCRAFT_REPORT_CODAR_AIREP, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(2, bulletin.get().getMessages().size());
        msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals("WXREP", msg.getMessageType().get().name());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(-1, 5, 55, ZoneId.of("Z"))), msg.getIssueTime().get());

        msg = bulletin.get().getMessages().get(1);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals("WXREP", msg.getMessageType().get().name());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(-1, 5, 55, ZoneId.of("Z"))), msg.getIssueTime().get());

    }

    @Test
    public void testSIGMETBulletinParsing() {
        ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("WSFI31 EFHK 310555\n" //
                        + "EFIN SIGMET 3 VALID 300830/301030 EFKL- EFIN FINLAND FIR SEV ICE (FZRA) "
                        + "OBS N6044 E02512 - N6051 E02821 - N6132 E02933 - N6325 E02743 - N6315 E02653 - N6045 E02510 SFC/4000FT MOV ENE 15KT NC=",
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.WARNINGS, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_SIGMET, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.SIGMET, msg.getMessageType().get());

        assertTrue(msg.getValidityTime().isPresent());
        final PartialOrCompleteTimeInstant start = PartialOrCompleteTimeInstant.of(PartialDateTime.of(30, 8, 30, ZoneId.of("Z")));
        final PartialOrCompleteTimeInstant end = PartialOrCompleteTimeInstant.of(PartialDateTime.of(30, 10, 30, ZoneId.of("Z")));
        assertEquals(PartialOrCompleteTimePeriod.builder().setStartTime(start).setEndTime(end).build(), msg.getValidityTime().get());

        result = this.converter.convertMessage("WSUS31 KKCI 051255 \n" //
                + "SIGE  \n" //
                + "CONVECTIVE SIGMET_START 11E \n" //
                + "VALID UNTIL 1455Z \n" //
                + "FL CSTL WTRS \n" //
                + "FROM 140SSW TLH-120W PIE-210S CEW-160S CEW-140SSW TLH \n" //
                + "AREA EMBD TS MOV FROM 27010KT. TOPS TO FL440. \n" //
                + " \n" //
                + "OUTLOOK VALID 051455-051855 \n" //
                + "FROM 40WSW GSO-80ESE ILM-80NE TRV-TRV-210WSW PIE-LEV-50NW \n" //
                + "SJI-AMG-40WSW GSO \n" //
                + "WST ISSUANCES EXPD. REFER TO MOST RECENT ACUS01 KWNS FROM STORM \n" //
                + "PREDICTION CENTER FOR SYNOPSIS AND METEOROLOGICAL DETAILS.=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(1, bulletin.get().getMessages().size());
        msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.SIGMET, msg.getMessageType().get());

        assertTrue(msg.getValidityTime().isPresent());
        assertTrue(msg.getValidityTime().get().getEndTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(-1, 14, 55, ZoneId.of("Z"))), msg.getValidityTime().get().getEndTime().get());
    }

    @Test
    public void testAIRMETBulletinParsing() {
        ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("WAFI31 EFHK 310555\n" //
                        + "EFIN AIRMET 3 VALID 300830/301030 EFKL- EFIN FINLAND FIR ISOL TSGR "
                        + "OBS N6044 E02512 - N6051 E02821 - N6132 E02933 - N6325 E02743 - N6315 E02653 - N6045 E02510 SFC/4000FT MOV ENE 15KT NC=",
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.WARNINGS, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_AIRMET, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.AIRMET, msg.getMessageType().get());

        assertTrue(msg.getValidityTime().isPresent());
        final PartialOrCompleteTimeInstant start = PartialOrCompleteTimeInstant.of(PartialDateTime.of(30, 8, 30, ZoneId.of("Z")));
        final PartialOrCompleteTimeInstant end = PartialOrCompleteTimeInstant.of(PartialDateTime.of(30, 10, 30, ZoneId.of("Z")));
        assertEquals(PartialOrCompleteTimePeriod.builder().setStartTime(start).setEndTime(end).build(), msg.getValidityTime().get());

    }

    @Test
    public void testSpaceWeatherBulletinParsing() {
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("FNXX01 EFKL 281200\n" //
                + "SWX ADVISORY\n" //
                + "STATUS: TEST\n"//
                + "DTG: 20190128/1200Z\n" //
                + "SWXC: PECASUS\n" //
                + "ADVISORY NR: 2019/1\n"//
                + "SWX EFFECT: SATCOM MOD\n" //
                + "OBS SWX: 08/1200Z NO SWX EXP\n"//
                + "FCST SWX +6 HR: 08/1800Z NO SWX EXP\n"//
                + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                + "FCST SWX +18 HR: 09/0600Z NO SWX EXP\n"//
                + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                + "RMK: TEST TEST TEST TEST\n" //
                + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" //
                + "SEE WWW.PECASUS.ORG \n" //
                + "NXT ADVISORY: NO FURTHER ADVISORIES\n \n", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.FORECASTS, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_SPACE_WEATHER, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        final GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.SPACE_WEATHER_ADVISORY, msg.getMessageType().get());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getCompleteTime().isPresent());
        assertEquals(ZonedDateTime.of(2019, 1, 28, 12, 0, 0, 0, ZoneId.of("Z")), msg.getIssueTime().get().getCompleteTime().get());

        assertTrue(msg.getValidityTime().isPresent());
        final PartialOrCompleteTimeInstant start = PartialOrCompleteTimeInstant.of(PartialDateTime.of(8, 12, 0, ZoneId.of("Z")));
        final PartialOrCompleteTimeInstant end = PartialOrCompleteTimeInstant.of(PartialDateTime.of(9, 12, 0, ZoneId.of("Z")));
        assertEquals(PartialOrCompleteTimePeriod.builder().setStartTime(start).setEndTime(end).build(), msg.getValidityTime().get());
    }

    @Test
    public void testVolcanicAshBulletinParsing() {
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("FVXX01 EFKL 281200\n" //
                + "VA ADVISORY\n" //
                + "DTG: 20190301/0045Z\n" //
                + "VAAC: DARWIN\n" //
                + "VOLCANO: DUKONO 268010\n" //
                + "PSN: N0141 E12753\n" //
                + "AREA: INDONESIA\n" //
                + "SUMMIT ELEV: 1335M\n" //
                + "ADVISORY NR: 2019/186\n" //
                + "INFO SOURCE: HIMAWARI-8\n" //
                + "AVIATION COLOUR CODE: ORANGE\n" //
                + "ERUPTION DETAILS: CONTINUOUS VA TO FL070\n" //
                + "OBS VA DTG: 01/0045Z\n" //
                + "OBS VA CLD: SFC/FL070 N0139 E12756 - N0144 E12756 - N0202\n" //
                + "E12732 - N0146 E12716 - N0128 E12725 - N0121 E12745 MOV W\n" //
                + "5KT\n" //
                + "FCST VA CLD +6 HR: 01/0645Z SFC/FL070 N0140 E12757 - N0118\n" //
                + "E12738 - N0121 E12710 - N0142 E12709 - N0154 E12733 - N0147\n" //
                + "E12753\n" //
                + "FCST VA CLD +12 HR: 01/1245Z SFC/FL070 N0145 E12752 - N0129\\n" //
                + "E12711 - N0109 E12722 - N0105 E12752 - N0141 E12758\n" //
                + "FCST VA CLD +18 HR: 01/1845Z SFC/FL070 N0146 E12754 - N0140\n" //
                + "E12758 - N0106 E12747 - N0116 E12714 - N0147 E12722 - N0147\n" //
                + "E12753\n" //
                + "RMK: VA DISCERNIBLE ON SATELLITE IMAGERY EXT FM NW TO SW. VA\n" //
                + "DIFFUSE DUE TO LIGHT WINDS. HEIGHT AND FCST BASED ON\n" //
                + "HIMAWARI 8, MENADO 28/1200Z SOUNDING AND MODEL GUIDANCE. LOW\n" //
                + "CONFIDENCE IN FCST DUE TO LIGHT/VAR WINDS.\n" //
                + "NXT ADVISORY: NO LATER THAN 20190301/0645Z\n" //
                + "\u0003\n", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.FORECASTS, bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_VOLCANIC_ASH_ADVISORIES, bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        final GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        assertTrue(msg.getMessageType().isPresent());
        assertEquals(MessageType.VOLCANIC_ASH_ADVISORY, msg.getMessageType().get());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getCompleteTime().isPresent());
        assertEquals(ZonedDateTime.of(2019, 3, 1, 0, 45, 0, 0, ZoneId.of("Z")), msg.getIssueTime().get().getCompleteTime().get());

        assertTrue(msg.getValidityTime().isPresent());
        final PartialOrCompleteTimeInstant start = PartialOrCompleteTimeInstant.of(PartialDateTime.of(1, 0, 45, ZoneId.of("Z")));
        final PartialOrCompleteTimeInstant end = PartialOrCompleteTimeInstant.of(PartialDateTime.of(1, 18, 45, ZoneId.of("Z")));
        assertEquals(PartialOrCompleteTimePeriod.builder().setStartTime(start).setEndTime(end).build(), msg.getValidityTime().get());
    }

    @Test
    public void testCustomBulletinParsing() {
        final ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("MFFI31 EFHK 310600\n" //
                + "WX WRNG EFHK 310600Z NIL=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(DataTypeDesignatorT1.fromCode('M'), bulletin.get().getHeading().getDataTypeDesignatorT1ForTAC());
        assertEquals(DataTypeDesignatorT2.fromExtensionCode('F'), bulletin.get().getHeading().getDataTypeDesignatorT2());
        assertEquals(1, bulletin.get().getMessages().size());
        final GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);

        Map<GenericAviationWeatherMessage.LocationIndicatorType, String> expectedIndicators =
                Collections.singletonMap(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, "EFHK");
        assertEquals(expectedIndicators, msg.getLocationIndicators());

        assertTrue(msg.getIssueTime().isPresent());
        assertTrue(msg.getIssueTime().get().getPartialTime().isPresent());
        assertEquals(PartialOrCompleteTimeInstant.of(PartialDateTime.of(31, 6, 0, ZoneId.of("Z"))), msg.getIssueTime().get());
    }
}
