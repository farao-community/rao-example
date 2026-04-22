package com.farao_community.farao.rao_example;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;
import java.util.Objects;

public class InInputs {

    public static void main(String[] args) throws IOException {
        // Import network from UCTE
        String networkFilename = "12Nodes.uct";
        Network network = Network.read(networkFilename, InInputs.class.getResourceAsStream("/%s".formatted(networkFilename)));

        // Import IN CRAC
        String cracFilename = "IN_inputs/cse_crac_1.xml";
        Crac crac = Crac.read(cracFilename, Objects.requireNonNull(InInputs.class.getResourceAsStream("/%s".formatted(cracFilename))), network);

        // Import JSON RAO parameters
        String raoParametersFilename = "IN_inputs/raoParameters_CSE_5_21_0.json";
        RaoParameters raoParameters = JsonRaoParameters.read(InInputs.class.getResourceAsStream("/%s".formatted(raoParametersFilename)));

        // Run RAO
        RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInputBuilder.build(), raoParameters);

        System.exit(0);
    }
}
