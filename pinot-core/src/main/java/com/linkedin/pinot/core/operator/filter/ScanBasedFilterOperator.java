/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.core.operator.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.pinot.core.common.BaseFilterBlock;
import com.linkedin.pinot.core.common.Block;
import com.linkedin.pinot.core.common.BlockDocIdValueSet;
import com.linkedin.pinot.core.common.BlockId;
import com.linkedin.pinot.core.common.BlockMetadata;
import com.linkedin.pinot.core.common.BlockValSet;
import com.linkedin.pinot.core.common.DataSource;
import com.linkedin.pinot.core.common.DataSourceMetadata;
import com.linkedin.pinot.core.common.FilterBlockDocIdSet;
import com.linkedin.pinot.core.common.Predicate;
import com.linkedin.pinot.core.operator.docidsets.ScanBasedMultiValueDocIdSet;
import com.linkedin.pinot.core.operator.docidsets.ScanBasedSingleValueDocIdSet;
import com.linkedin.pinot.core.operator.filter.predicate.PredicateEvaluator;
import com.linkedin.pinot.core.operator.filter.predicate.PredicateEvaluatorProvider;
import com.linkedin.pinot.core.segment.index.readers.Dictionary;


public class ScanBasedFilterOperator extends BaseFilterOperator {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScanBasedFilterOperator.class);

  private DataSource dataSource;

  public ScanBasedFilterOperator(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean open() {
    dataSource.open();
    return true;
  }

  @Override
  public BaseFilterBlock nextFilterBlock(BlockId BlockId) {
    Predicate predicate = getPredicate();
    Dictionary dictionary = dataSource.getDictionary();
    DataSourceMetadata dataSourceMetadata = dataSource.getDataSourceMetadata();
    FilterBlockDocIdSet docIdSet;
    Block nextBlock = dataSource.nextBlock();
    BlockValSet blockValueSet = nextBlock.getBlockValueSet();
    BlockMetadata blockMetadata = nextBlock.getMetadata();
    PredicateEvaluator evaluator = PredicateEvaluatorProvider.getPredicateFunctionFor(predicate, dictionary);
    if (dataSourceMetadata.isSingleValue()) {
      docIdSet = new ScanBasedSingleValueDocIdSet(blockValueSet, blockMetadata, evaluator.getDictionaryIds());
    } else {
      docIdSet = new ScanBasedMultiValueDocIdSet(blockValueSet, blockMetadata, evaluator.getDictionaryIds());
    }

    return new ScanBlock(docIdSet);
  }

  @Override
  public boolean close() {
    dataSource.close();
    return true;
  }

  public static class ScanBlock extends BaseFilterBlock {

    private FilterBlockDocIdSet docIdSet;

    public ScanBlock(FilterBlockDocIdSet docIdSet) {
      this.docIdSet = docIdSet;
    }

    @Override
    public BlockId getId() {
      return new BlockId(0);
    }

    @Override
    public boolean applyPredicate(Predicate predicate) {
      throw new UnsupportedOperationException("applypredicate not supported in " + this.getClass());
    }



    @Override
    public BlockValSet getBlockValueSet() {
      throw new UnsupportedOperationException("getBlockValueSet not supported in " + this.getClass());
    }

    @Override
    public BlockDocIdValueSet getBlockDocIdValueSet() {
      throw new UnsupportedOperationException("getBlockDocIdValueSet not supported in " + this.getClass());
    }

    @Override
    public BlockMetadata getMetadata() {
      throw new UnsupportedOperationException("getMetadata not supported in " + this.getClass());
    }

    @Override
    public FilterBlockDocIdSet getFilteredBlockDocIdSet() {
      return docIdSet;
    }

  }

}
