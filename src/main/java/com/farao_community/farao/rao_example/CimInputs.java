package com.farao_community.farao.rao_example;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class CimInputs {

    public static void main(String[] args) throws URISyntaxException, IOException {
        // Import network from CIM CGMES file
        String networkFilename = "CIM_inputs/MicroGrid.zip";
        Properties importParams = new Properties(); // TODO: is this compulsory and documented?
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        Path path = Paths.get(Objects.requireNonNull(Main.class.getResource("/%s".formatted(networkFilename))).toURI());
        Network network = Network.read(path, LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        // Import CIM CRAC
        String cracFilename = "CIM_inputs/CIM_21_7_1.xml"; // TODO: make it work
        Crac crac = Crac.read(cracFilename, Objects.requireNonNull(Main.class.getResourceAsStream("/%s".formatted(cracFilename))), network);

        // Import JSON RAO parameters
        String raoParametersFilename = "IN_inputs/raoParameters_SWE_5_19_2.json";
        RaoParameters raoParameters = JsonRaoParameters.read(Main.class.getResourceAsStream("/%s".formatted(raoParametersFilename)));

        // Run RAO
        RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInputBuilder.build(), raoParameters);

        System.exit(0);
    }

}
