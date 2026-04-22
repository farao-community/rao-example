package com.farao_community.farao.rao_example;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters;

public class LoadflowRun {

    public static void main(String[] args) throws IOException {
        // Import network from UCTE
        String networkFilename = "12Nodes.uct";
        Network network = Network.read(networkFilename, LoadflowRun.class.getResourceAsStream("/%s".formatted(networkFilename)));

        // Import LoadFlowParameters from RaoParameters
        String raoParametersFilename = "IN_inputs/raoParameters_CSE_5_21_0.json";
        RaoParameters raoParameters = JsonRaoParameters.read(LoadflowRun.class.getResourceAsStream("/%s".formatted(raoParametersFilename)));
        LoadFlowParameters loadFlowParameters = getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters();

        LoadFlow.run(network, loadFlowParameters);
    }
}
