package com.farao_community.farao.rao_example;

import com.farao_community.farao.cse.import_runner.app.dichotomy.CseD2ccShiftDispatcher;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.google.common.collect.ImmutableMap;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.cse.CseGlskDocument;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.iidm.network.Country.*;

public class ShiftRun {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShiftRun.class);

    public static void main(String[] args) throws IOException, GlskLimitationException, ShiftingException {
        // Import network from UCTE
        String networkFilename = "IN_inputs/NETWORK_TEST_IN.uct";
        Network network = Network.read(networkFilename, ShiftRun.class.getResourceAsStream("/%s".formatted(networkFilename)));

        String glskDocumentFilename = "IN_inputs/GLSK_PROP_IN.xml";
        GlskDocument glskDocument = CseGlskDocument.importGlsk(ShiftRun.class.getResourceAsStream("/%s".formatted(glskDocumentFilename)), false, true);
        ZonalData<Scalable> zonalScalable = glskDocument.getZonalScalable(network);

        Map<String, Double> reducedSplittingFactors = ImmutableMap.of(
                new CountryEICode(FR).getCode(), 0.4,
                new CountryEICode(AT).getCode(), 0.3,
                new CountryEICode(CH).getCode(), 0.1,
                new CountryEICode(SI).getCode(), 0.2
        );

        Map<String, Double> ntcs = ImmutableMap.of(
                new CountryEICode(FR).getCode(), 400.,
                new CountryEICode(AT).getCode(), 300.,
                new CountryEICode(CH).getCode(), 100.,
                new CountryEICode(SI).getCode(), 200.
        );
        double netPositionShift = 300;

        ShiftDispatcher shiftDispatcher = new CseD2ccShiftDispatcher(LOGGER, reducedSplittingFactors, ntcs);
        LinearScaler linearScaler = new LinearScaler(zonalScalable, shiftDispatcher);
        linearScaler.shiftNetwork(netPositionShift, network);

        System.exit(0);
    }
}
