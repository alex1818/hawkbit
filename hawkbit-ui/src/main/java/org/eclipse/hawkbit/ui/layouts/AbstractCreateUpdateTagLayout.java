/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.layouts;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerConstants;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerHelper;
import org.eclipse.hawkbit.ui.colorpicker.ColorPickerLayout;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIWindowDecorator;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;
import com.vaadin.ui.components.colorpicker.ColorSelector;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract class for create/update target tag layout.
 */
public abstract class AbstractCreateUpdateTagLayout extends CustomComponent
        implements ColorChangeListener, ColorSelector {
    private static final long serialVersionUID = 4229177824620576456L;
    private static final String TAG_NAME_DYNAMIC_STYLE = "new-tag-name";
    private static final String TAG_DESC_DYNAMIC_STYLE = "new-tag-desc";
    protected static final String TAG_DYNAMIC_STYLE = "tag-color-preview";
    protected static final String MESSAGE_ERROR_MISSING_TAGNAME = "message.error.missing.tagname";

    @Autowired
    protected I18N i18n;

    @Autowired
    protected transient TagManagement tagManagement;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    @Autowired
    protected SpPermissionChecker permChecker;

    @Autowired
    protected transient UINotification uiNotification;

    private final FormLayout formLayout = new FormLayout();

    protected String createTagStr;
    protected String updateTagStr;
    protected Label comboLabel;
    protected CommonDialogWindow window;

    protected Label colorLabel;
    protected TextField tagName;
    protected TextArea tagDesc;
    protected Button tagColorPreviewBtn;
    protected OptionGroup optiongroup;
    protected ComboBox tagNameComboBox;

    protected VerticalLayout comboLayout;
    protected ColorPickerLayout colorPickerLayout;
    protected GridLayout mainLayout;
    protected VerticalLayout contentLayout;

    protected boolean tagPreviewBtnClicked;

    private String colorPicked;
    protected String tagNameValue;
    protected String tagDescValue;

    protected abstract String getWindowCaption();

    /**
     * Save new tag / update new tag.
     *
     * @param event
     */
    protected abstract void save(final Button.ClickEvent event);

    /**
     * Discard the changes and close the popup.
     *
     * @param event
     */
    protected void discard(final Button.ClickEvent event) {
        UI.getCurrent().removeWindow(window);
    }

    /**
     * Populate target name combo.
     */
    protected abstract void populateTagNameCombo();

    protected abstract void setTagDetails(final String tagSelected);

    /**
     * Init the layout.
     */
    public void init() {

        setSizeUndefined();
        createRequiredComponents();
        buildLayout();
        addListeners();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    protected void createRequiredComponents() {

        createTagStr = i18n.get("label.create.tag");
        updateTagStr = i18n.get("label.update.tag");
        comboLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.tag"), null);
        colorLabel = SPUIComponentProvider.getLabel(i18n.get("label.choose.tag.color"), null);
        colorLabel.addStyleName(SPUIDefinitions.COLOR_LABEL_STYLE);

        tagName = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_NAME, true, "", i18n.get("textfield.name"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        tagName.setId(SPUIDefinitions.NEW_TARGET_TAG_NAME);

        tagDesc = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "",
                ValoTheme.TEXTFIELD_TINY + " " + SPUIDefinitions.TAG_DESC, false, "", i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        tagDesc.setId(SPUIDefinitions.NEW_TARGET_TAG_DESC);
        tagDesc.setImmediate(true);
        tagDesc.setNullRepresentation("");

        tagNameComboBox = SPUIComponentProvider.getComboBox(null, "", "", null, null, false, "",
                i18n.get("label.combobox.tag"));
        tagNameComboBox.addStyleName(SPUIDefinitions.FILTER_TYPE_COMBO_STYLE);
        tagNameComboBox.setImmediate(true);
        tagNameComboBox.setId(SPUIComponentIdProvider.DIST_TAG_COMBO);

        tagColorPreviewBtn = new Button();
        tagColorPreviewBtn.setId(SPUIComponentIdProvider.TAG_COLOR_PREVIEW_ID);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        tagColorPreviewBtn.setStyleName(TAG_DYNAMIC_STYLE);
    }

    protected void buildLayout() {

        mainLayout = new GridLayout(3, 2);
        mainLayout.setSpacing(true);
        comboLayout = new VerticalLayout();
        colorPickerLayout = new ColorPickerLayout();
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        contentLayout = new VerticalLayout();

        final HorizontalLayout colorLabelLayout = new HorizontalLayout();
        colorLabelLayout.setMargin(false);
        colorLabelLayout.addComponents(colorLabel, tagColorPreviewBtn);

        formLayout.addComponent(optiongroup);
        formLayout.addComponent(comboLayout);
        formLayout.addComponent(tagName);
        formLayout.addComponent(tagDesc);
        formLayout.addStyleName("form-lastrow");
        formLayout.setSizeFull();

        contentLayout.addComponent(formLayout);
        contentLayout.addComponent(colorLabelLayout);
        contentLayout.setComponentAlignment(formLayout, Alignment.MIDDLE_CENTER);
        contentLayout.setComponentAlignment(colorLabelLayout, Alignment.MIDDLE_LEFT);
        contentLayout.setSizeUndefined();

        mainLayout.setSizeFull();
        mainLayout.addComponent(contentLayout, 0, 0);

        colorPickerLayout.setVisible(false);
        mainLayout.addComponent(colorPickerLayout, 1, 0);
        mainLayout.setComponentAlignment(colorPickerLayout, Alignment.MIDDLE_CENTER);

        setCompositionRoot(mainLayout);
        tagName.focus();
    }

    protected void addListeners() {
        colorPickerLayout.getColorSelect().addColorChangeListener(this);
        colorPickerLayout.getSelPreview().addColorChangeListener(this);
        tagColorPreviewBtn.addClickListener(event -> previewButtonClicked());
        tagNameComboBox.addValueChangeListener(this::tagNameChosen);
        slidersValueChangeListeners();
    }

    /**
     * Open color picker on click of preview button. Auto select the color based
     * on target tag if already selected.
     */
    protected void previewButtonClicked() {
        if (!tagPreviewBtnClicked) {
            setColor();
        }

        tagPreviewBtnClicked = !tagPreviewBtnClicked;
        colorPickerLayout.setVisible(tagPreviewBtnClicked);
    }

    private void setColor() {
        final String selectedOption = (String) optiongroup.getValue();
        if (selectedOption == null || !selectedOption.equalsIgnoreCase(updateTagStr)) {
            return;
        }

        if (tagNameComboBox.getValue() == null) {
            colorPickerLayout
                    .setSelectedColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
            return;
        }

        final TargetTag targetTagSelected = tagManagement.findTargetTag(tagNameComboBox.getValue().toString());

        if (targetTagSelected == null) {
            final DistributionSetTag distTag = tagManagement
                    .findDistributionSetTag(tagNameComboBox.getValue().toString());
            colorPickerLayout.setSelectedColor(
                    distTag.getColour() != null ? ColorPickerHelper.rgbToColorConverter(distTag.getColour())
                            : ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
        } else {
            colorPickerLayout.setSelectedColor(targetTagSelected.getColour() != null
                    ? ColorPickerHelper.rgbToColorConverter(targetTagSelected.getColour())
                    : ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
        }
    }

    private void tagNameChosen(final ValueChangeEvent event) {
        final String tagSelected = (String) event.getProperty().getValue();
        if (null != tagSelected) {
            setTagDetails(tagSelected);
        } else {
            resetTagNameField();
        }
        window.setOrginaleValues();
    }

    protected void resetTagNameField() {
        tagName.setEnabled(false);
        tagName.clear();
        tagDesc.clear();
        restoreComponentStyles();
        colorPickerLayout.setSelectedColor(colorPickerLayout.getDefaultColor());
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        tagPreviewBtnClicked = false;
    }

    /**
     * Listener for option group - Create tag/Update.
     *
     * @param event
     *            ValueChangeEvent
     */
    protected void optionValueChanged(final ValueChangeEvent event) {

        if (updateTagStr.equals(event.getProperty().getValue())) {
            tagName.clear();
            tagDesc.clear();
            tagName.setEnabled(false);
            populateTagNameCombo();
            // show target name combo
            comboLayout.addComponent(comboLabel);
            comboLayout.addComponent(tagNameComboBox);
        } else {
            tagName.setEnabled(true);
            tagName.clear();
            tagDesc.clear();
            // hide target name combo
            comboLayout.removeComponent(comboLabel);
            comboLayout.removeComponent(tagNameComboBox);
        }
        // close the color picker layout
        tagPreviewBtnClicked = false;
        // reset the selected color - Set default color
        restoreComponentStyles();
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
        colorPickerLayout.getSelPreview()
                .setColor(ColorPickerHelper.rgbToColorConverter(ColorPickerConstants.DEFAULT_COLOR));
        window.setOrginaleValues();
    }

    /**
     * reset the components.
     */
    protected void reset() {
        tagName.setEnabled(true);
        tagName.clear();
        tagDesc.clear();
        restoreComponentStyles();

        // hide target name combo
        comboLayout.removeComponent(comboLabel);
        comboLayout.removeComponent(tagNameComboBox);

        // Default green color
        colorPickerLayout.setVisible(false);
        colorPickerLayout.setSelectedColor(colorPickerLayout.getDefaultColor());
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        tagPreviewBtnClicked = false;
    }

    /**
     * On change of color in color picker ,change RGB sliders, components border
     * color and color of preview button.
     */
    @Override
    public void colorChanged(final ColorChangeEvent event) {
        setColor(event.getColor());
        for (final ColorSelector select : colorPickerLayout.getSelectors()) {
            if (!event.getSource().equals(select) && select.equals(this)
                    && !select.getColor().equals(colorPickerLayout.getSelectedColor())) {
                select.setColor(colorPickerLayout.getSelectedColor());
            }
        }
        ColorPickerHelper.setRgbSliderValues(colorPickerLayout);
        getPreviewButtonColor(event.getColor().getCSS());
        createDynamicStyleForComponents(tagName, tagDesc, event.getColor().getCSS());
    }

    /**
     * Dynamic styles for window.
     *
     * @param top
     *            int value
     * @param marginLeft
     *            int value
     */
    protected void getPreviewButtonColor(final String color) {
        Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.getPreviewButtonColorScript(color));
    }

    /**
     * Set tag name and desc field border color based on chosen color.
     *
     * @param tagName
     * @param tagDesc
     * @param taregtTagColor
     */
    protected void createDynamicStyleForComponents(final TextField tagName, final TextArea tagDesc,
            final String taregtTagColor) {

        tagName.removeStyleName(SPUIDefinitions.TAG_NAME);
        tagDesc.removeStyleName(SPUIDefinitions.TAG_DESC);
        getTargetDynamicStyles(taregtTagColor);
        tagName.addStyleName(TAG_NAME_DYNAMIC_STYLE);
        tagDesc.addStyleName(TAG_DESC_DYNAMIC_STYLE);
    }

    /**
     * reset the tag name and tag description component border color.
     */
    protected void restoreComponentStyles() {
        tagName.removeStyleName(TAG_NAME_DYNAMIC_STYLE);
        tagDesc.removeStyleName(TAG_DESC_DYNAMIC_STYLE);
        tagName.addStyleName(SPUIDefinitions.TAG_NAME);
        tagDesc.addStyleName(SPUIDefinitions.TAG_DESC);
        getPreviewButtonColor(ColorPickerConstants.DEFAULT_COLOR);
    }

    /**
     * Get target style - Dynamically as per the color picked, cannot be done
     * from the static css.
     *
     * @param colorPickedPreview
     */
    private void getTargetDynamicStyles(final String colorPickedPreview) {
        Page.getCurrent().getJavaScript()
                .execute(HawkbitCommonUtil.changeToNewSelectedPreviewColor(colorPickedPreview));
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public void setColor(final Color color) {
        if (color == null) {
            return;
        }
        colorPickerLayout.setSelectedColor(color);
        colorPickerLayout.getSelPreview().setColor(colorPickerLayout.getSelectedColor());
        final String colorPickedPreview = colorPickerLayout.getSelPreview().getColor().getCSS();
        if (tagName.isEnabled() && null != colorPickerLayout.getColorSelect()) {
            createDynamicStyleForComponents(tagName, tagDesc, colorPickedPreview);
            colorPickerLayout.getColorSelect().setColor(colorPickerLayout.getSelPreview().getColor());
        }

    }

    /**
     * create option group with Create tag/Update tag based on permissions.
     */
    protected void createOptionGroup(final boolean hasCreatePermission, final boolean hasUpdatePermission) {

        optiongroup = new OptionGroup("Select Action");
        optiongroup.setId(SPUIComponentIdProvider.OPTION_GROUP);
        optiongroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        optiongroup.addStyleName("custom-option-group");
        optiongroup.setNullSelectionAllowed(false);

        if (hasCreatePermission) {
            optiongroup.addItem(createTagStr);
        }
        if (hasUpdatePermission) {
            optiongroup.addItem(updateTagStr);
        }

        setOptionGroupDefaultValue(hasCreatePermission, hasUpdatePermission);
    }

    protected void setOptionGroupDefaultValue(final boolean hasCreatePermission, final boolean hasUpdatePermission) {

        if (hasCreatePermission) {
            optiongroup.select(createTagStr);
        }
        if (hasUpdatePermission && !hasCreatePermission) {
            optiongroup.select(updateTagStr);
        }
    }

    public ColorPickerLayout getColorPickerLayout() {
        return colorPickerLayout;
    }

    public CommonDialogWindow getWindow() {
        reset();
        window = SPUIWindowDecorator.getWindow(getWindowCaption(), null, SPUIDefinitions.CREATE_UPDATE_WINDOW, this,
                this::save, this::discard, null, mainLayout, i18n);
        return window;
    }

    /**
     * Value change listeners implementations of sliders.
     */
    private void slidersValueChangeListeners() {
        colorPickerLayout.getRedSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = -8336732888800920839L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double red = (Double) event.getProperty().getValue();
                final Color newColor = new Color((int) red, colorPickerLayout.getSelectedColor().getGreen(),
                        colorPickerLayout.getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        colorPickerLayout.getGreenSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1236358037766775663L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double green = (Double) event.getProperty().getValue();
                final Color newColor = new Color(colorPickerLayout.getSelectedColor().getRed(), (int) green,
                        colorPickerLayout.getSelectedColor().getBlue());
                setColorToComponents(newColor);
            }
        });
        colorPickerLayout.getBlueSlider().addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8466370763686043947L;

            @Override
            public void valueChange(final ValueChangeEvent event) {
                final double blue = (Double) event.getProperty().getValue();
                final Color newColor = new Color(colorPickerLayout.getSelectedColor().getRed(),
                        colorPickerLayout.getSelectedColor().getGreen(), (int) blue);
                setColorToComponents(newColor);
            }
        });
    }

    protected void setColorToComponents(final Color newColor) {
        setColor(newColor);
        colorPickerLayout.getColorSelect().setColor(newColor);
        getPreviewButtonColor(newColor.getCSS());
        createDynamicStyleForComponents(tagName, tagDesc, newColor.getCSS());
    }

    /**
     * Create new tag.
     */
    protected void createNewTag() {
        colorPicked = ColorPickerHelper.getColorPickedString(colorPickerLayout.getSelPreview());
        tagNameValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        tagDescValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());
    }

    /**
     * update tag.
     */
    protected void updateExistingTag(final Tag targetObj) {
        final String nameUpdateValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagName.getValue());
        final String descUpdateValue = HawkbitCommonUtil.trimAndNullIfEmpty(tagDesc.getValue());

        if (null != nameUpdateValue) {
            targetObj.setName(nameUpdateValue);
            targetObj.setDescription(null != descUpdateValue ? descUpdateValue : null);
            targetObj.setColour(ColorPickerHelper.getColorPickedString(colorPickerLayout.getSelPreview()));
            if (targetObj instanceof TargetTag) {
                tagManagement.updateTargetTag((TargetTag) targetObj);
            } else if (targetObj instanceof DistributionSetTag) {
                tagManagement.updateDistributionSetTag((DistributionSetTag) targetObj);
            }
            uiNotification.displaySuccess(i18n.get("message.update.success", new Object[] { targetObj.getName() }));
        } else {
            uiNotification.displayValidationError(i18n.get("message.tag.update.mandatory"));
        }
    }

    protected void displaySuccess(final String tagName) {
        uiNotification.displaySuccess(i18n.get("message.save.success", new Object[] { tagName }));
    }

    protected void displayValidationError(final String errorMessage) {
        uiNotification.displayValidationError(errorMessage);
    }

    protected void setTagColor(final Color selectedColor, final String previewColor) {
        getColorPickerLayout().setSelectedColor(selectedColor);
        getColorPickerLayout().getSelPreview().setColor(getColorPickerLayout().getSelectedColor());
        getColorPickerLayout().getColorSelect().setColor(getColorPickerLayout().getSelectedColor());
        createDynamicStyleForComponents(tagName, tagDesc, previewColor);
        getPreviewButtonColor(previewColor);
    }

    protected Boolean checkIsDuplicate(final Tag existingTag) {
        if (existingTag != null) {
            displayValidationError(i18n.get("message.tag.duplicate.check", new Object[] { existingTag.getName() }));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public String getColorPicked() {
        return colorPicked;
    }

    public void setColorPicked(final String colorPicked) {
        this.colorPicked = colorPicked;
    }

    public String getTagNameValue() {
        return tagNameValue;
    }

    public void setTagNameValue(final String tagNameValue) {
        this.tagNameValue = tagNameValue;
    }

    public String getTagDescValue() {
        return tagDescValue;
    }

    public void setTagDescValue(final String tagDescValue) {
        this.tagDescValue = tagDescValue;
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

    public GridLayout getMainLayout() {
        return mainLayout;
    }

    @Override
    public void addColorChangeListener(final ColorChangeListener listener) {
    }

    @Override
    public void removeColorChangeListener(final ColorChangeListener listener) {
    }

}
