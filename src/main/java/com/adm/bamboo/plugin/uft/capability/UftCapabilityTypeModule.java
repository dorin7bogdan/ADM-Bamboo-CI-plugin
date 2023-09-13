/*
 * Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 * This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 * Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * Copyright 2012-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors ("Open Text") are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ___________________________________________________________________
 */

package com.adm.bamboo.plugin.uft.capability;

import com.adm.bamboo.plugin.uft.helpers.locator.UFTLocatorService;
import com.adm.bamboo.plugin.uft.helpers.locator.UFTLocatorServiceFactory;
import com.adm.utils.uft.StringUtils;
import com.atlassian.bamboo.v2.build.agent.capability.*;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UftCapabilityTypeModule extends AbstractExecutableCapabilityTypeModule {

    public static final String MF_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".OpenText";
    public static final String MF_UFT_CAPABILITY = MF_PREFIX + ".UFT One";
    public static final String UFT_EXECUTABLE = "uft";
    public static final String UFT_EXECUTABLE_KEY = "uftPath";

    // we need this to access the input parameter given by the configurator context, in getCapability method while leveraging manual adding possibility
    // just for convenience
    private static final String FIELD_UFT_PATH = "uftPath";
    private static final String FIELD_UFT_DETECTION = "uftDetection";

    // unused, but mandatory
    private static final String CAPABILITY_TYPE_ERROR_UNDEFINED_EXECUTABLE = AGENT_CAPABILITY_TYPE_PREFIX + MF_PREFIX + ".undefinedExecutable";

    private final UFTLocatorService locatorService;

    public UftCapabilityTypeModule() {
        locatorService = UFTLocatorServiceFactory.getInstance().getLocator();
    }

    // automatic identification of capabilities, only works if the server and agent machine is the same
    @NotNull
    @Override
    public CapabilitySet addDefaultCapabilities(@NotNull CapabilitySet capabilitySet) {
        if (locatorService.isInstalled()) {
            final String uftPath = locatorService.getUftExeFullPath();

            if (!uftPath.isEmpty()) {
                capabilitySet.addCapability(new CapabilityImpl(MF_UFT_CAPABILITY, uftPath));

                return capabilitySet;
            }

            capabilitySet.removeCapability(MF_UFT_CAPABILITY);
        }

        return capabilitySet;
    }

    // validation of user input
    @NotNull
    @Override
    public Map<String, String> validate(@NotNull Map<String, String[]> params) {
        final boolean detectionFlag = !StringUtils.isNullOrEmpty(getParamValue(params, FIELD_UFT_DETECTION));
        Map<String, String> errors = new HashMap<>();

        /*
        * If, we're using remote agents that run on different machines, we cannot check at this point, if the UFT path is valid or not,
        * we must move the validation process to the Task's responsibility,
        * reason: at this point we are not connected to the remote machine's file system
        * */

        if (detectionFlag) {
            final String givenPath = getParamValue(params, FIELD_UFT_PATH);

            if (StringUtils.isNullOrEmpty(givenPath)) {
                errors.put(FIELD_UFT_PATH, getText("MFCapability.unspecifiedPath"));
                return errors;
            }

            if (givenPath.contains(".")) {
                errors.put(FIELD_UFT_PATH, getText("MFCapability.executableErr"));
            }
        } else {
            if (!locatorService.isInstalled()) {
                errors.put(FIELD_UFT_DETECTION, getText("MFCapability.notInstalled"));
            }
        }

        return errors;
    }

    // user input capability
    @NotNull
    @Override
    public Capability getCapability(@NotNull Map<String, String[]> params) {
        final boolean detectionFlag = !StringUtils.isNullOrEmpty(getParamValue(params, FIELD_UFT_DETECTION));

        String uftPath;
        if (detectionFlag) {
            uftPath = locatorService.getPathFromManualPoint(getParamValue(params, FIELD_UFT_PATH));
        } else {
            uftPath = locatorService.getUftExeFullPath();
        }

        return new CapabilityImpl(MF_UFT_CAPABILITY, uftPath);
    }

    // necessary overloads, many times unused
    @Override
    public String getMandatoryCapabilityKey() {
        return MF_UFT_CAPABILITY;
    }

    @Override
    public String getExecutableKey() {
        return UFT_EXECUTABLE_KEY;
    }

    @Override
    public String getCapabilityUndefinedKey() {
        return CAPABILITY_TYPE_ERROR_UNDEFINED_EXECUTABLE;
    }

    @Override
    public List<String> getDefaultWindowPaths() {
        return Lists.newArrayList();
    }

    @Override
    public String getExecutableFilename() {
        return UFT_EXECUTABLE;
    }

    // necessary methods to return property texts which then by a method call be shown on the Bamboo UI
    public String getExecutableDescription() {
        return getText("MFCapability.descExec");
    }
    public String getDetectionDescription() {
        return getText("MFCapability.descDetection");
    }

}
