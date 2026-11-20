package com.ledgerflow.insights;

import com.ledgerflow.ledger.domain.Category;
import com.ledgerflow.ledger.domain.LedgerEntry.Direction;
import java.math.BigDecimal;

record FlowAmount(Direction direction, Category category, BigDecimal total) { }
