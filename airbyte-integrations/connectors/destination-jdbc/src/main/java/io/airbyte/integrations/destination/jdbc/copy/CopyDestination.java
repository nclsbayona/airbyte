/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.integrations.destination.jdbc.copy;

import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.db.jdbc.JdbcDatabase;
import io.airbyte.integrations.BaseConnector;
import io.airbyte.integrations.base.Destination;
import io.airbyte.integrations.destination.ExtendedNameTransformer;
import io.airbyte.integrations.destination.jdbc.AbstractJdbcDestination;
import io.airbyte.integrations.destination.jdbc.SqlOperations;
import io.airbyte.protocol.models.AirbyteConnectionStatus;
import io.airbyte.protocol.models.ConnectorSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CopyDestination extends BaseConnector implements Destination {

  private static final Logger LOGGER = LoggerFactory.getLogger(CopyDestination.class);

  public abstract void attemptWriteToPersistence(JsonNode config) throws Exception;

  public abstract ExtendedNameTransformer getNameTransformer();

  public abstract JdbcDatabase getDatabase(JsonNode config) throws Exception;

  public abstract SqlOperations getSqlOperations();

  @Override
  public AirbyteConnectionStatus check(JsonNode config) {
    try {
      attemptWriteToPersistence(config);

      var nameTransformer = getNameTransformer();
      var outputSchema = nameTransformer.convertStreamName(config.get("schema").asText());
      JdbcDatabase database = getDatabase(config);
      AbstractJdbcDestination.attemptSQLCreateAndDropTableOperations(outputSchema, database, nameTransformer, getSqlOperations());

      return new AirbyteConnectionStatus().withStatus(AirbyteConnectionStatus.Status.SUCCEEDED);
    } catch (Exception e) {
      LOGGER.error("Exception while checking connection: ", e);
      return new AirbyteConnectionStatus()
          .withStatus(AirbyteConnectionStatus.Status.FAILED)
          .withMessage("Could not connect with provided configuration. \n" + e.getMessage());
    }
  }

}
