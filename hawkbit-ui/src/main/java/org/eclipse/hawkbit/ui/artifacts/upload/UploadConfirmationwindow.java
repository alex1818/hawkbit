/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.artifacts.state.CustomFile;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleTiny;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Artifact upload confirmation popup.
 * 
 */
public class UploadConfirmationwindow implements Button.ClickListener {

    private static final long serialVersionUID = -1679035890140031740L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadConfirmationwindow.class);

    private static final String MD5_CHECKSUM = "md5Checksum";

    private static final String SHA1_CHECKSUM = "sha1Checksum";

    private static final String FILE_NAME = "fileName";

    private static final String SW_MODULE_NAME = "swModuleName";

    private static final String SIZE = "size";

    private static final String ACTION = "action";

    private static final String BASE_SOFTWARE_ID = "softwareModuleId";

    private static final String FILE_NAME_LAYOUT = "fileNameLayout";

    private static final String WARNING_ICON = "warningIcon";

    private static final String CUSTOM_FILE = "customFile";

    private static final String ARTIFACT_UPLOAD_EXCEPTION = "Artifact upload exception:";

    private static final String ALREADY_EXISTS_MSG = "upload.artifact.alreadyExists";

    private final I18N i18n;

    private Window uploadConfrimationWindow;

    private Button uploadBtn;

    private Button cancelBtn;

    private Table uploadDetailsTable;

    private final UploadLayout uploadLayout;

    private IndexedContainer tabelContainer;

    private final List<UploadStatus> uploadResultList = new ArrayList<>();

    private VerticalLayout uploadArtifactDetails;

    private UploadResultWindow currentUploadResultWindow;

    private int redErrorLabelCount = 0;

    private final ArtifactUploadState artifactUploadState;

    /**
     * Initialize the upload confirmation window.
     * 
     * @param artifactUploadView
     *            reference of upload layout.
     * @param artifactUploadState
     *            reference of session variable {@link ArtifactUploadState}.
     */
    public UploadConfirmationwindow(final UploadLayout artifactUploadView,
            final ArtifactUploadState artifactUploadState) {
        this.uploadLayout = artifactUploadView;
        this.artifactUploadState = artifactUploadState;
        i18n = artifactUploadView.getI18n();
        createRequiredComponents();
        buildLayout();
    }

    private Boolean checkIfArtifactDetailsDispalyed(final Long bSoftwareModuleId) {
        if (artifactUploadState.getSelectedBaseSoftwareModule().isPresent()
                && artifactUploadState.getSelectedBaseSoftwareModule().get().getId().equals(bSoftwareModuleId)) {
            return true;
        }
        return false;
    }

    private Boolean preUploadValidation(final List<String> itemIds) {
        Boolean validationSuccess = true;
        for (final String itemId : itemIds) {
            final Item item = tabelContainer.getItem(itemId);
            final String providedFileName = (String) item.getItemProperty(FILE_NAME).getValue();
            if (HawkbitCommonUtil.trimAndNullIfEmpty(providedFileName) == null) {
                validationSuccess = false;
                break;
            }
        }
        return validationSuccess;
    }

    private void createRequiredComponents() {
        uploadBtn = SPUIComponentProvider.getButton(SPUIComponentIdProvider.UPLOAD_BUTTON, SPUILabelDefinitions.SUBMIT,
                SPUILabelDefinitions.SUBMIT, ValoTheme.BUTTON_PRIMARY, false, null, SPUIButtonStyleTiny.class);
        uploadBtn.addClickListener(this);
        cancelBtn = SPUIComponentProvider.getButton(SPUIComponentIdProvider.UPLOAD_DISCARD_DETAILS_BUTTON,
                SPUILabelDefinitions.DISCARD, SPUILabelDefinitions.DISCARD, null, false, null,
                SPUIButtonStyleTiny.class);
        cancelBtn.addClickListener(this);

        uploadDetailsTable = new Table();
        uploadDetailsTable.addStyleName("artifact-table");
        uploadDetailsTable.setSizeFull();
        uploadDetailsTable.setId(SPUIComponentIdProvider.UPLOAD_ARTIFACT_DETAILS_TABLE);
        uploadDetailsTable.addStyleName(ValoTheme.TABLE_BORDERLESS);
        uploadDetailsTable.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        uploadDetailsTable.addStyleName(ValoTheme.TABLE_SMALL);

        setTableContainer();
        populateUploadDetailsTable();
    }

    /**
     * Warning icon is displayed, if an artifact exists with same provided file
     * name. Error icon is displayed ,if file name entered is duplicate.
     *
     * @param warningIconLabel
     *            warning/error label
     * @param fileName
     *            provided file name
     * @param itemId
     *            item id of the current row
     */
    private void setWarningIcon(final Label warningIconLabel, final String fileName, final Object itemId) {
        final Item item = uploadDetailsTable.getItem(itemId);
        final ArtifactManagement artifactManagement = SpringContextHelper.getBean(ArtifactManagement.class);
        if (HawkbitCommonUtil.trimAndNullIfEmpty(fileName) != null) {
            final Long baseSwId = (Long) item.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final List<LocalArtifact> artifactList = artifactManagement.findByFilenameAndSoftwareModule(fileName,
                    baseSwId);
            if (!artifactList.isEmpty()) {
                warningIconLabel.setVisible(true);
                if (isErrorIcon(warningIconLabel)) {
                    warningIconLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    redErrorLabelCount--;
                }
                warningIconLabel.setDescription(i18n.get(ALREADY_EXISTS_MSG));
                if (checkForDuplicate(fileName, itemId, baseSwId)) {
                    warningIconLabel.setDescription(i18n.get("message.duplicate.filename"));
                    warningIconLabel.addStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    redErrorLabelCount++;
                }
            } else {
                warningIconLabel.setVisible(false);
                if (warningIconLabel.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
                    warningIconLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    warningIconLabel.setDescription(i18n.get(ALREADY_EXISTS_MSG));
                    redErrorLabelCount--;
                }
            }
        }
    }

    private Boolean checkForDuplicate(final String fileName, final Object itemId, final Long currentBaseSwId) {
        for (final Object newItemId : tabelContainer.getItemIds()) {
            final Item newItem = tabelContainer.getItem(newItemId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String newFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!newItemId.equals(itemId) && newBaseSwId.equals(currentBaseSwId) && newFileName.equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    private void populateUploadDetailsTable() {
        for (final CustomFile customFile : uploadLayout.getFileSelected()) {
            final String swNameVersion = HawkbitCommonUtil.getFormattedNameVersion(
                    customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
            final String itemId = swNameVersion + "/" + customFile.getFileName();
            final Item newItem = tabelContainer.addItem(itemId);
            final SoftwareModule bSoftwareModule = artifactUploadState.getBaseSwModuleList().get(swNameVersion);
            newItem.getItemProperty(BASE_SOFTWARE_ID).setValue(bSoftwareModule.getId());

            addFileNameLayout(newItem, swNameVersion, customFile.getFileName(), itemId);

            newItem.getItemProperty(SW_MODULE_NAME).setValue(HawkbitCommonUtil.getFormatedLabel(swNameVersion));
            newItem.getItemProperty(SIZE).setValue(customFile.getFileSize());
            final Button deleteIcon = SPUIComponentProvider.getButton(
                    SPUIComponentIdProvider.UPLOAD_DELETE_ICON + "-" + itemId, "", SPUILabelDefinitions.DISCARD,
                    ValoTheme.BUTTON_TINY + " " + "redicon", true, FontAwesome.TRASH_O,
                    SPUIButtonStyleSmallNoBorder.class);
            deleteIcon.addClickListener(this);
            deleteIcon.setData(itemId);
            newItem.getItemProperty(ACTION).setValue(deleteIcon);

            final TextField sha1 = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                    null, true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
            sha1.setId(swNameVersion + "/" + customFile.getFileName() + "/sha1");

            final TextField md5 = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, false, null,
                    null, true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
            md5.setId(swNameVersion + "/" + customFile.getFileName() + "/md5");

            final TextField customFileName = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY,
                    false, null, null, true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
            customFileName.setId(swNameVersion + "/" + customFile.getFileName() + "/customFileName");
            newItem.getItemProperty(SHA1_CHECKSUM).setValue(sha1);
            newItem.getItemProperty(MD5_CHECKSUM).setValue(md5);
            newItem.getItemProperty(CUSTOM_FILE).setValue(customFile);
        }
    }

    private void addFileNameLayout(final Item newItem, final String baseSoftwareModuleNameVersion,
            final String customFileName, final String itemId) {
        final HorizontalLayout horizontalLayout = new HorizontalLayout();
        final TextField fileNameTextField = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY,
                false, null, null, true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        fileNameTextField.setId(baseSoftwareModuleNameVersion + "/" + customFileName + "/customFileName");
        fileNameTextField.setData(baseSoftwareModuleNameVersion + "/" + customFileName);
        fileNameTextField.setValue(customFileName);

        newItem.getItemProperty(FILE_NAME).setValue(fileNameTextField.getValue());

        final Label warningIconLabel = getWarningLabel();
        warningIconLabel.setId(baseSoftwareModuleNameVersion + "/" + customFileName + "/icon");
        setWarningIcon(warningIconLabel, fileNameTextField.getValue(), itemId);
        newItem.getItemProperty(WARNING_ICON).setValue(warningIconLabel);

        horizontalLayout.addComponent(fileNameTextField);
        horizontalLayout.setComponentAlignment(fileNameTextField, Alignment.MIDDLE_LEFT);
        horizontalLayout.addComponent(warningIconLabel);
        horizontalLayout.setComponentAlignment(warningIconLabel, Alignment.MIDDLE_RIGHT);
        newItem.getItemProperty(FILE_NAME_LAYOUT).setValue(horizontalLayout);

        fileNameTextField.addTextChangeListener(event -> onFileNameChange(event, warningIconLabel, newItem));
    }

    private void onFileNameChange(final TextChangeEvent event, final Label warningIconLabel, final Item newItem) {

        final String itemId = (String) ((TextField) event.getComponent()).getData();
        final String fileName = event.getText();

        final Boolean isWarningIconDisplayed = isWarningIcon(warningIconLabel);
        setWarningIcon(warningIconLabel, fileName, itemId);

        final Long currentSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
        final String oldFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
        newItem.getItemProperty(FILE_NAME).setValue(event.getText());

        // if warning was displayed prior and not displayed currently
        if (isWarningIconDisplayed && !warningIconLabel.isVisible()) {
            modifyIconOfSameSwId(itemId, currentSwId, oldFileName);
        }
        checkDuplicateEntry(itemId, currentSwId, event.getText(), oldFileName);
        enableOrDisableUploadBtn();
    }

    private void enableOrDisableUploadBtn() {
        if (redErrorLabelCount == 0) {
            uploadBtn.setEnabled(true);
        } else {
            uploadBtn.setEnabled(false);
        }
    }

    /**
     * If warning was displayed prior and not displayed currently ,the update
     * other warning labels accordingly.
     *
     * @param itemId
     *            id of row which is deleted/whose file name modified.
     * @param oldSwId
     *            software module id
     * @param oldFileName
     *            file name before modification
     */
    private void modifyIconOfSameSwId(final Object itemId, final Long oldSwId, final String oldFileName) {
        for (final Object rowId : tabelContainer.getItemIds()) {
            final Item newItem = tabelContainer.getItem(rowId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String newFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!rowId.equals(itemId) && newBaseSwId.equals(oldSwId) && newFileName.equals(oldFileName)) {
                final HorizontalLayout layout = (HorizontalLayout) newItem.getItemProperty(FILE_NAME_LAYOUT).getValue();
                final Label warningLabel = (Label) layout.getComponent(1);
                if (warningLabel.isVisible()) {
                    warningLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    warningLabel.setDescription(i18n.get(ALREADY_EXISTS_MSG));
                    newItem.getItemProperty(WARNING_ICON).setValue(warningLabel);
                    redErrorLabelCount--;
                    break;
                }
            }
        }

    }

    /**
     * Check if icon is warning icon and visible.
     *
     * @param icon
     *            label
     * @return Boolean
     */
    private Boolean isWarningIcon(final Label icon) {
        if (icon.isVisible() && !icon.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
            return true;
        }
        return false;
    }

    /**
     * Check if icon is error icon and visible.
     *
     * @param icon
     *            label
     * @return Boolean
     */
    private Boolean isErrorIcon(final Label icon) {
        if (icon.isVisible() && icon.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
            return true;
        }
        return false;
    }

    private Label getWarningLabel() {
        final Label warningIconLabel = new Label();
        warningIconLabel.addStyleName(ValoTheme.LABEL_SMALL);
        warningIconLabel.setHeightUndefined();
        warningIconLabel.setContentMode(ContentMode.HTML);
        warningIconLabel.setValue(FontAwesome.WARNING.getHtml());
        warningIconLabel.addStyleName("warningLabel");
        warningIconLabel.setVisible(false);
        return warningIconLabel;
    }

    private void newFileNameIsDuplicate(final Object itemId, final Long currentSwId, final String currentChangedText) {
        for (final Object rowId : tabelContainer.getItemIds()) {
            final Item currentItem = tabelContainer.getItem(itemId);
            final Item newItem = tabelContainer.getItem(rowId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String fileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!rowId.equals(itemId) && newBaseSwId.equals(currentSwId) && fileName.equals(currentChangedText)) {
                final HorizontalLayout layout = (HorizontalLayout) currentItem.getItemProperty(FILE_NAME_LAYOUT)
                        .getValue();
                final Label iconLabel = (Label) layout.getComponent(1);
                if (!iconLabel.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
                    iconLabel.setVisible(true);
                    iconLabel.setDescription(i18n.get("message.duplicate.filename"));
                    iconLabel.addStyleName(SPUIStyleDefinitions.ERROR_LABEL);
                    redErrorLabelCount++;
                }
                break;
            }
        }
    }

    private void reValidateOtherFileNamesOfSameBaseSw(final Object itemId, final Long currentSwId,
            final String oldFileName) {
        Label warningLabel = null;
        Label errorLabel = null;
        int errorLabelCount = 0;
        int duplicateCount = 0;
        for (final Object rowId : tabelContainer.getItemIds()) {
            final Item newItem = tabelContainer.getItem(rowId);
            final Long newBaseSwId = (Long) newItem.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final String newFileName = (String) newItem.getItemProperty(FILE_NAME).getValue();
            if (!rowId.equals(itemId) && newBaseSwId.equals(currentSwId) && newFileName.equals(oldFileName)) {
                final HorizontalLayout layout = (HorizontalLayout) newItem.getItemProperty(FILE_NAME_LAYOUT).getValue();
                final Label icon = (Label) layout.getComponent(1);
                duplicateCount++;
                if (icon.isVisible()) {
                    if (!icon.getStyleName().contains(SPUIStyleDefinitions.ERROR_LABEL)) {
                        warningLabel = icon;
                        break;
                    }
                    errorLabel = icon;
                    errorLabelCount++;
                }

            }
        }
        hideErrorIcon(warningLabel, errorLabelCount, duplicateCount, errorLabel, oldFileName, currentSwId);
    }

    private void hideErrorIcon(final Label warningLabel, final int errorLabelCount, final int duplicateCount,
            final Label errorLabel, final String oldFileName, final Long currentSwId) {
        if (warningLabel == null && (errorLabelCount > 1 || duplicateCount == 1 && errorLabelCount == 1)) {
            final ArtifactManagement artifactManagement = SpringContextHelper.getBean(ArtifactManagement.class);
            final List<LocalArtifact> artifactList = artifactManagement.findByFilenameAndSoftwareModule(oldFileName,
                    currentSwId);
            errorLabel.removeStyleName(SPUIStyleDefinitions.ERROR_LABEL);
            errorLabel.setDescription(i18n.get(ALREADY_EXISTS_MSG));
            if (artifactList.isEmpty()) {
                errorLabel.setVisible(false);
            }
            redErrorLabelCount--;
        }
    }

    private void checkDuplicateEntry(final Object itemId, final Long currentSwId, final String newChangedText,
            final String oldFileName) {
        /**
         * Check if newly entered file name is a duplicate.
         */
        newFileNameIsDuplicate(itemId, currentSwId, newChangedText);
        /**
         * After the current changed file name is validated ,other files of same
         * software module as has be revalidated. And icons are updated
         * accordingly.
         */
        reValidateOtherFileNamesOfSameBaseSw(itemId, currentSwId, oldFileName);

    }

    private void setTableContainer() {
        tabelContainer = new IndexedContainer();
        tabelContainer.addContainerProperty(FILE_NAME_LAYOUT, HorizontalLayout.class, null);
        tabelContainer.addContainerProperty(SW_MODULE_NAME, Label.class, null);
        tabelContainer.addContainerProperty(SHA1_CHECKSUM, TextField.class, null);
        tabelContainer.addContainerProperty(MD5_CHECKSUM, TextField.class, null);
        tabelContainer.addContainerProperty(SIZE, Long.class, null);
        tabelContainer.addContainerProperty(ACTION, Button.class, "");
        tabelContainer.addContainerProperty(FILE_NAME, String.class, null);
        tabelContainer.addContainerProperty(BASE_SOFTWARE_ID, Long.class, null);
        tabelContainer.addContainerProperty(WARNING_ICON, Label.class, null);
        tabelContainer.addContainerProperty(CUSTOM_FILE, CustomFile.class, null);

        uploadDetailsTable.setContainerDataSource(tabelContainer);
        uploadDetailsTable.setPageLength(10);
        uploadDetailsTable.setColumnHeader(FILE_NAME_LAYOUT, i18n.get("upload.file.name"));
        uploadDetailsTable.setColumnHeader(SW_MODULE_NAME, i18n.get("upload.swModuleTable.header"));
        uploadDetailsTable.setColumnHeader(SHA1_CHECKSUM, i18n.get("upload.sha1"));
        uploadDetailsTable.setColumnHeader(MD5_CHECKSUM, i18n.get("upload.md5"));
        uploadDetailsTable.setColumnHeader(SIZE, i18n.get("upload.size"));
        uploadDetailsTable.setColumnHeader(ACTION, i18n.get("upload.action"));

        uploadDetailsTable.setColumnExpandRatio(FILE_NAME_LAYOUT, 0.25f);
        uploadDetailsTable.setColumnExpandRatio(SW_MODULE_NAME, 0.17f);
        uploadDetailsTable.setColumnExpandRatio(SHA1_CHECKSUM, 0.2f);
        uploadDetailsTable.setColumnExpandRatio(MD5_CHECKSUM, 0.2f);
        uploadDetailsTable.setColumnExpandRatio(SIZE, 0.12f);
        uploadDetailsTable.setColumnExpandRatio(ACTION, 0.06f);

        final Object[] visibileColumn = { FILE_NAME_LAYOUT, SW_MODULE_NAME, SHA1_CHECKSUM, MD5_CHECKSUM, SIZE, ACTION };
        uploadDetailsTable.setVisibleColumns(visibileColumn);
    }

    private void buildLayout() {
        final HorizontalLayout footer = getFooterLayout();

        uploadArtifactDetails = new VerticalLayout();
        uploadArtifactDetails.setWidth(SPUIDefinitions.MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH + "px");
        uploadArtifactDetails.addStyleName("confirmation-popup");
        uploadArtifactDetails.addComponent(uploadDetailsTable);
        uploadArtifactDetails.setComponentAlignment(uploadDetailsTable, Alignment.MIDDLE_CENTER);
        uploadArtifactDetails.addComponent(footer);
        uploadArtifactDetails.setComponentAlignment(footer, Alignment.MIDDLE_CENTER);

        uploadConfrimationWindow = new Window();
        uploadConfrimationWindow.setContent(uploadArtifactDetails);
        uploadConfrimationWindow.setResizable(Boolean.FALSE);
        uploadConfrimationWindow.setClosable(Boolean.TRUE);
        uploadConfrimationWindow.setDraggable(Boolean.TRUE);
        uploadConfrimationWindow.setModal(true);
        uploadConfrimationWindow.addCloseListener(event -> onPopupClose());
        uploadConfrimationWindow.setCaption(i18n.get("header.caption.upload.details"));
        uploadConfrimationWindow.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);
    }

    private void onPopupClose() {
        uploadLayout.setCurrentUploadConfirmationwindow(null);
    }

    private HorizontalLayout getFooterLayout() {
        final HorizontalLayout footer = new HorizontalLayout();
        footer.setSizeUndefined();
        footer.addStyleName("confirmation-window-footer");
        footer.setSpacing(true);
        footer.setMargin(false);
        footer.addComponents(uploadBtn, cancelBtn);
        footer.setComponentAlignment(uploadBtn, Alignment.TOP_LEFT);
        footer.setComponentAlignment(cancelBtn, Alignment.TOP_RIGHT);
        return footer;
    }

    public Window getUploadConfrimationWindow() {
        return uploadConfrimationWindow;
    }

    @Override
    public void buttonClick(final ClickEvent event) {
        if (event.getComponent().getId().equals(SPUIComponentIdProvider.UPLOAD_ARTIFACT_DETAILS_CLOSE)) {
            uploadConfrimationWindow.close();
        } else if (event.getComponent().getId().equals(SPUIComponentIdProvider.UPLOAD_DISCARD_DETAILS_BUTTON)) {
            uploadLayout.clearUploadedFileDetails();
            uploadConfrimationWindow.close();
        } else if (event.getComponent().getId().equals(SPUIComponentIdProvider.UPLOAD_BUTTON)) {
            processArtifactUpload();
        }

        else if (event.getComponent().getId().startsWith(SPUIComponentIdProvider.UPLOAD_DELETE_ICON)) {
            final String itemId = ((Button) event.getComponent()).getData().toString();
            final Item item = uploadDetailsTable.getItem(((Button) event.getComponent()).getData());
            final Long swId = (Long) item.getItemProperty(BASE_SOFTWARE_ID).getValue();
            final CustomFile customFile = (CustomFile) item.getItemProperty(CUSTOM_FILE).getValue();
            final String fileName = (String) item.getItemProperty(FILE_NAME).getValue();
            final Label warningIconLabel = (Label) item.getItemProperty(WARNING_ICON).getValue();
            final Boolean isWarningIconDisplayed = isWarningIcon(warningIconLabel);
            if (isWarningIconDisplayed) {
                modifyIconOfSameSwId(itemId, swId, fileName);
            } else if (isErrorIcon(warningIconLabel)) {
                redErrorLabelCount--;
            }
            reValidateOtherFileNamesOfSameBaseSw(((Button) event.getComponent()).getData(), swId, fileName);
            enableOrDisableUploadBtn();

            uploadDetailsTable.removeItem(((Button) event.getComponent()).getData());
            uploadLayout.getFileSelected().remove(customFile);
            uploadLayout.updateUploadCounts();
            if (uploadDetailsTable.getItemIds().isEmpty()) {
                uploadConfrimationWindow.close();
                uploadLayout.clearUploadedFileDetails();
            }

        }

    }

    private void processArtifactUpload() {
        final List<String> itemIds = (List<String>) uploadDetailsTable.getItemIds();
        if (preUploadValidation(itemIds)) {
            final ArtifactManagement artifactManagement = SpringContextHelper.getBean(ArtifactManagement.class);
            Boolean refreshArtifactDetailsLayout = false;
            for (final String itemId : itemIds) {
                final String[] itemDet = itemId.split("/");
                final String baseSoftwareModuleNameVersion = itemDet[0];
                final String fileName = itemDet[1];
                final SoftwareModule bSoftwareModule = artifactUploadState.getBaseSwModuleList()
                        .get(baseSoftwareModuleNameVersion);
                for (final CustomFile customFile : uploadLayout.getFileSelected()) {
                    final String baseSwModuleNameVersion = HawkbitCommonUtil.getFormattedNameVersion(
                            customFile.getBaseSoftwareModuleName(), customFile.getBaseSoftwareModuleVersion());
                    if (customFile.getFileName().equals(fileName)
                            && baseSwModuleNameVersion.equals(baseSoftwareModuleNameVersion)) {
                        createLocalArtifact(itemId, customFile.getFilePath(), artifactManagement, bSoftwareModule);
                    }
                }
                refreshArtifactDetailsLayout = checkIfArtifactDetailsDispalyed(bSoftwareModule.getId());
            }
            if (refreshArtifactDetailsLayout) {
                uploadLayout.refreshArtifactDetailsLayout(artifactUploadState.getSelectedBaseSoftwareModule().get());
            }
            uploadLayout.clearFileList();
            uploadConfrimationWindow.close();
            // call upload result window
            currentUploadResultWindow = new UploadResultWindow(uploadResultList, i18n);
            UI.getCurrent().addWindow(currentUploadResultWindow.getUploadResultsWindow());
            currentUploadResultWindow.getUploadResultsWindow().addCloseListener(event -> onResultDetailsPopupClose());
            uploadLayout.setResultPopupHeightWidth(Page.getCurrent().getBrowserWindowWidth(),
                    Page.getCurrent().getBrowserWindowHeight());
        } else {
            uploadLayout.getUINotification()
                    .displayValidationError(uploadLayout.getI18n().get("message.error.noProvidedName"));
        }

    }

    private void onResultDetailsPopupClose() {
        currentUploadResultWindow = null;
    }

    private void createLocalArtifact(final String itemId, final String filePath,
            final ArtifactManagement artifactManagement, final SoftwareModule baseSw) {
        final File newFile = new File(filePath);
        final Item item = tabelContainer.getItem(itemId);
        final String sha1Checksum = ((TextField) item.getItemProperty(SHA1_CHECKSUM).getValue()).getValue();
        final String md5Checksum = ((TextField) item.getItemProperty(MD5_CHECKSUM).getValue()).getValue();
        final String providedFileName = (String) item.getItemProperty(FILE_NAME).getValue();
        final CustomFile customFile = (CustomFile) item.getItemProperty(CUSTOM_FILE).getValue();
        final String[] itemDet = itemId.split("/");
        final String swModuleNameVersion = itemDet[0];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(newFile);
            artifactManagement.createLocalArtifact(fis, baseSw.getId(), providedFileName,
                    HawkbitCommonUtil.trimAndNullIfEmpty(md5Checksum),
                    HawkbitCommonUtil.trimAndNullIfEmpty(sha1Checksum), true, customFile.getMimeType());
            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.SUCCESS, "");
        } catch (final FileNotFoundException e) {
            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.FAILED, e.getMessage());
            LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);
        } catch (final ArtifactUploadFailedException e) {
            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.FAILED, e.getMessage());
            LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);
        } catch (final InvalidSHA1HashException e) {
            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.FAILED, e.getMessage());
            LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);
        } catch (final InvalidMD5HashException e) {
            saveUploadStatus(providedFileName, swModuleNameVersion, SPUILabelDefinitions.FAILED, e.getMessage());
            LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);
        } finally {
            closeFileStream(fis, newFile);
        }
    }

    private void saveUploadStatus(final String fileName, final String baseSwModuleName, final String status,
            final String message) {
        final UploadStatus result = new UploadStatus();
        result.setFileName(fileName);
        result.setBaseSwModuleName(baseSwModuleName);
        result.setUploadResult(status);
        result.setReason(message);
        uploadResultList.add(result);

    }

    private void closeFileStream(final FileInputStream fis, final File newFile) {

        if (fis != null) {
            try {
                fis.close();
            } catch (final IOException e) {
                LOG.error(ARTIFACT_UPLOAD_EXCEPTION, e);
            }
        }
        if (newFile.exists() && !newFile.delete()) {
            LOG.error("Could not delete temporary file: {}", newFile);
        }

    }

    public Table getUploadDetailsTable() {
        return uploadDetailsTable;
    }

    public VerticalLayout getUploadArtifactDetails() {
        return uploadArtifactDetails;
    }

    public UploadResultWindow getCurrentUploadResultWindow() {
        return currentUploadResultWindow;
    }

    public void setCurrentUploadResultWindow(final UploadResultWindow currentUploadResultWindow) {
        this.currentUploadResultWindow = currentUploadResultWindow;
    }

}
