/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.impl.dao.test;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIManagerDatabaseException;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationServiceImpl;
import org.wso2.carbon.apimgt.impl.certificatemgt.CertificateManagementException;
import org.wso2.carbon.apimgt.impl.dao.CertificateMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.CertificateMetadataDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.base.MultitenantConstants;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * This method holds test cases for the class CertificateMgtDAO.
 * Junit @FixMethodOrder is used to order the test methods, so that the method names are in lexicographic order.
 * https://github.com/junit-team/junit4/blob/master/doc/ReleaseNotes4.11.md#test-execution-order
 */
public class CertificateMgtDaoTest {

    private static CertificateMgtDAO certificateMgtDAO;
    private static String TEST_ALIAS = "test alias";
    private static String TEST_ALIAS_2 = "test alias 2";
    private static String TEST_ENDPOINT = "test end point";
    private static String TEST_ENDPOINT_2 = "test end point 2";
    private static int TENANT_ID = MultitenantConstants.SUPER_TENANT_ID;
    private static final int TENANT_2 = 1001;

    @BeforeClass
    public static void setUp() throws APIManagerDatabaseException, APIManagementException, SQLException {
        String dbConfigPath = System.getProperty("APIManagerDBConfigurationPath");
        APIManagerConfiguration config = new APIManagerConfiguration();
        initializeDatabase(dbConfigPath);
        config.load(dbConfigPath);
        ServiceReferenceHolder.getInstance().setAPIManagerConfigurationService(new APIManagerConfigurationServiceImpl
                (config));
        APIMgtDBUtil.initialize();
        certificateMgtDAO = CertificateMgtDAO.getInstance();
    }

    private static void initializeDatabase(String configFilePath) {

        InputStream in;
        try {
            in = FileUtils.openInputStream(new File(configFilePath));
            StAXOMBuilder builder = new StAXOMBuilder(in);
            String dataSource = builder.getDocumentElement().getFirstChildWithName(new QName("DataSourceName"))
                    .getText();
            OMElement databaseElement = builder.getDocumentElement()
                    .getFirstChildWithName(new QName("Database"));
            String databaseURL = databaseElement.getFirstChildWithName(new QName("URL")).getText();
            String databaseUser = databaseElement.getFirstChildWithName(new QName("Username")).getText();
            String databasePass = databaseElement.getFirstChildWithName(new QName("Password")).getText();
            String databaseDriver = databaseElement.getFirstChildWithName(new QName("Driver")).getText();

            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName(databaseDriver);
            basicDataSource.setUrl(databaseURL);
            basicDataSource.setUsername(databaseUser);
            basicDataSource.setPassword(databasePass);

            // Create initial context
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.naming.java.javaURLContextFactory");
            System.setProperty(Context.URL_PKG_PREFIXES,
                    "org.apache.naming");
            try {
                InitialContext.doLookup("java:/comp/env/jdbc/WSO2AM_DB");
            } catch (NamingException e) {
                InitialContext ic = new InitialContext();
                ic.createSubcontext("java:");
                ic.createSubcontext("java:/comp");
                ic.createSubcontext("java:/comp/env");
                ic.createSubcontext("java:/comp/env/jdbc");

                ic.bind("java:/comp/env/jdbc/WSO2AM_DB", basicDataSource);
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIsTableExists() {
        Assert.assertTrue(certificateMgtDAO.isTableExists());
    }

    @Test
    public void testGetCertificateWithNoCertificate() {
        CertificateMetadataDTO certificateDTO = certificateMgtDAO.getCertificate(TEST_ALIAS_2, TEST_ENDPOINT_2, TENANT_ID);
        Assert.assertNull(certificateDTO);
    }

    @Test
    public void testAddCertificate() throws CertificateManagementException {
        boolean result = certificateMgtDAO.addCertificate(TEST_ALIAS, TEST_ENDPOINT, TENANT_ID);
        Assert.assertTrue(result);
    }

    @Test
    public void testGetCertificate() {
        CertificateMetadataDTO certificateDTO = certificateMgtDAO.getCertificate("ALIAS_1", "EP_1", TENANT_2);
        Assert.assertNotNull(certificateDTO);
    }

    @Test
    public void testDeleteCertificate() {
        boolean result = certificateMgtDAO.deleteCertificate("ALIAS2", "EP2", TENANT_ID);
        Assert.assertTrue(result);
    }

    @Test
    public void testGetCertificates() {
        List<CertificateMetadataDTO> certificates = certificateMgtDAO.getCertificates(TENANT_ID);
        Assert.assertNotNull(certificates);
        Assert.assertTrue(certificates.size() > 0);
    }

    @Test(expected = CertificateManagementException.class)
    public void testAddExistingCertificate() throws CertificateManagementException {
        certificateMgtDAO.addCertificate("ALIAS4", "EP4", TENANT_ID);
    }

    @Test(expected = CertificateManagementException.class)
    public void testAddExistingCertificateTenant() throws CertificateManagementException {
        certificateMgtDAO.addCertificate("ALIAS_3", "EP_3", TENANT_2);
    }
}

