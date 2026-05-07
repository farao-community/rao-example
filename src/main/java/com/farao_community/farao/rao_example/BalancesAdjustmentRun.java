package com.farao_community.farao.rao_example;

import com.powsybl.balances_adjustment.balance_computation.*;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.EICode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BalancesAdjustmentRun {
    public static void main(String[] args) throws IOException {
        // Import network from XIIDM
        String networkFilename = "CIM_inputs/MicroGrid.xiidm";
        Network network = Network.read(networkFilename, BalancesAdjustmentRun.class.getResourceAsStream("/%s".formatted(networkFilename)));

        String glskDocumentFilename = "CIM_inputs/GlskB45MicroGridTest.xml";
        GlskDocument glskDocument = CimGlskDocument.importGlsk(BalancesAdjustmentRun.class.getResourceAsStream("/%s".formatted(glskDocumentFilename)));
        ZonalData<Scalable> zonalScalable = glskDocument.getZonalScalable(network);

        Map<String, Integer> targetNetPositions = new HashMap<>();
        targetNetPositions.put("NL", 1000);
        targetNetPositions.put("BE", 1000);

        List<BalanceComputationArea> areas = createBalanceComputationAreas(targetNetPositions, zonalScalable);

        BalanceComputationFactory balanceComputationFactory = new BalanceComputationFactoryImpl();

        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        BalanceComputationParameters balanceComputationParameters = BalanceComputationParameters.load().setLoadFlowParameters(loadFlowParameters);
        ScalingParameters scalingParameters = ScalingParameters.load();
        scalingParameters.setAllowsGeneratorOutOfActivePowerLimits(true);
        scalingParameters.setPriority(ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED);
        balanceComputationParameters.setScalingParameters(scalingParameters);

        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        String variantId = network.getVariantManager().getWorkingVariantId();
        balanceComputation.run(network, variantId, balanceComputationParameters).join();

        System.exit(0);
    }

    private static List<BalanceComputationArea> createBalanceComputationAreas(Map<String, Integer> targetNetPositions, ZonalData<Scalable> zonalScalable) {
        return targetNetPositions.keySet().stream()
                .sorted().map(countryString -> { // sorted for consistency in tests
                    Country country = Country.valueOf(countryString);
                    NetworkAreaFactory networkAreaFactory = new CountryAreaFactory(country);
                    String areaCode = new EICode(country).getAreaCode();
                    Scalable scalable = zonalScalable.getData(areaCode);
                    if (Objects.isNull(scalable)) {
                        throw new RuntimeException("No scalable data found for country " + countryString);
                    }
                    return new BalanceComputationArea(countryString, networkAreaFactory, scalable, targetNetPositions.get(countryString));
                }).toList();
    }
}
