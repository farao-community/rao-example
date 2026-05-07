package com.farao_community.farao.rao_example;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.crac.io.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;

public class CimInputs {

    public static void main(String[] args) throws URISyntaxException, IOException {
        // Import network from CIM CGMES file
        String networkFilename = "CIM_inputs/MicroGrid.zip";
        Properties importParams = new Properties(); // Extra code necessary to import CIM CGMES files
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        Path path = Paths.get(Objects.requireNonNull(CimInputs.class.getResource("/%s".formatted(networkFilename))).toURI());
        Network network = Network.read(path, LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);

        // Import CIM CRAC
        String cracFilename = "CIM_inputs/CIM_21_7_1.xml";
        InputStream is = CimInputs.class.getResourceAsStream("/%s".formatted(cracFilename));
        OffsetDateTime offsetDateTime = ZonedDateTime.of(LocalDateTime.parse("2021-04-02 09:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), ZoneId.of("Europe/Brussels"))
                .toOffsetDateTime();
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(offsetDateTime);
        CimCracCreationContext cracCreationContext = (CimCracCreationContext) Crac.readWithContext(cracFilename, is, network, cracCreationParameters);
        Crac crac = cracCreationContext.getCrac();

        // Import JSON RAO parameters
        String raoParametersFilename = "CIM_inputs/raoParameters_SWE_5_21_0.json";
        RaoParameters raoParameters = JsonRaoParameters.read(CimInputs.class.getResourceAsStream("/%s".formatted(raoParametersFilename)));

        // Run RAO
        RaoInput.RaoInputBuilder raoInputBuilder = RaoInput.build(network, crac);
        RaoResult raoResult = Rao.find("SearchTreeRao").run(raoInputBuilder.build(), raoParameters);

        System.exit(0);
    }

}
