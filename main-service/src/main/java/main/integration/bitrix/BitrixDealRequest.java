package main.integration.bitrix;

import java.math.BigDecimal;

public record BitrixDealRequest(
        String title,
        String stageId,
        String comments,
        BigDecimal opportunity,
        String currencyId
) {
}
