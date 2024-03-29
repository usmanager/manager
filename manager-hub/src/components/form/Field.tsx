/*
 * MIT License
 *
 * Copyright (c) 2020 manager
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

import React from "react";
import {mapLabelToIcon} from "../../utils/image";
import {camelCaseToSentenceCase} from "../../utils/text";
import M from "materialize-css";
import {FormContext, IErrors, IFormContext, IValues} from "./Form";
import {TextBox} from "./TextBox";
import {MultilineTextBox} from "./MultilineTextBox";
import {Datepicker} from "./Datepicker";
import {Timepicker} from "./Timepicker";
import {NumberBox} from "./NumberBox";
import {Dropdown} from "./Dropdown";
import {CheckboxList} from "./CheckboxList";
import checkboxListStyles from "./CheckboxList.module.css";
import {Link} from "react-router-dom";
import LocationMap from "../map/LocationMap";
import {IMarker} from "../map/Marker";
import LoadingSpinner from "../list/LoadingSpinner";
import {Checkbox} from "./Checkbox";
import styles from './Form.module.css';

export interface IValidation {
    rule: (values: IValues, id: keyof IValues, args: any) => string;
    args?: any;
}

export interface FieldProps<T = string> {
    id: string;
    type?: "text" | "number" | "date" | "datepicker" | "timepicker" | "multilinetext" | "dropdown" | "list" | "map" | "checkbox";
    label?: string;
    value?: any;
    valueToString?: (v: T) => string;
    dropdown?: { defaultValue: string, values: T[], optionToString?: (v: T) => string, selectCallback?: (value: any) => void, emptyMessage?: string };
    number?: { min: number, max: number };
    validation?: IValidation | boolean;
    icon?: { include?: boolean, name?: string, linkedTo?: ((v: T) => string | null) | string };
    disabled?: boolean;
    hidden?: boolean;
    map?: {
        loading: boolean, editable?: boolean, singleMarker?: boolean, zoomable?: boolean, labeled?: boolean, markers?: IMarker[],
        valueToMarkers?: (v: T[]) => IMarker[]
    }
    checkbox?: { label?: string, checkCallback?: (value: any) => void }
}

export const getTypeFromValue = (value: any): 'text' | 'number' =>
    value === undefined
    || value === null
    || value === ''
    || typeof value === 'boolean'
    || (typeof value === 'string' && !value.trim().length)
    || isNaN(value)
        ? 'text'
        : 'number';

export default class Field<T> extends React.Component<FieldProps<T>, {}> {

    public componentDidMount(): void {
        this.updateField();
    }

    public componentDidUpdate(prevProps: Readonly<FieldProps<T>>, prevState: Readonly<{}>, snapshot?: any): void {
        this.updateField();
    }

    private linkedIcon = (label: string, iconName: string | undefined, linkedTo: ((v: T) => string | null) | string, value: any): JSX.Element => {
        const icon = <>{iconName ? iconName : mapLabelToIcon(label, value)}</>;
        const link = typeof linkedTo === 'string' ? linkedTo : linkedTo(value)
        return link ? <Link to={link}>{icon}</Link> : icon;
    }

    public render() {
        const {id, type, label, dropdown, number, icon, disabled, hidden, valueToString, map, checkbox, validation} = this.props;
        const getError = (errors: IErrors): string => (errors ? errors[id] : '');
        const getEditorClassname = (errors: IErrors, disabled: boolean, value: string): string => {
            const hasErrors = getError(errors);
            if (hasErrors) {
                return "invalidate-field";
            } else if (!hasErrors && !disabled && (getTypeFromValue(value) !== 'text' || value) && validation !== false) {
                return "validate-field";
            } else {
                return "no-validation-field";
            }
        };
        return (
            <FormContext.Consumer>
                {(formContext: IFormContext | null) => (
                    formContext && (
                        <div key={id}
                             className={type !== 'list' ? `input-field col s12` : `input-field col s12 ${checkboxListStyles.listWrapper}`}
                             style={icon?.include !== undefined && !icon?.include ? {marginLeft: '10px'} : undefined}>
                            {label && type !== "list" && (
                                <>
                                    {(icon?.include === undefined || icon?.include) &&
                                    <i className={`material-icons prefix ${styles.labelIcon}`}>
                                        {icon?.linkedTo
                                            ? this.linkedIcon(label, icon.name, icon.linkedTo, formContext.values[id])
                                            : icon?.name
                                                ? icon.name
                                                : type?.toLowerCase() !== 'checkbox' && mapLabelToIcon(label, formContext.values[id])}
                                    </i>}
                                    <label className="active" htmlFor={id}>
                                        {camelCaseToSentenceCase(label)}
                                        {type?.toLowerCase() !== 'checkbox' && formContext.isRequired(id) &&
                                        <label className={`red-text ${styles.requiredLabel}`}>*</label>}
                                    </label>
                                </>
                            )}
                            {(!type || type.toLowerCase() === "text") && (
                                <TextBox<T>
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={formContext.values[id]}
                                    disabled={disabled || !formContext.isEditing}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id])}
                                    valueToString={valueToString}
                                    onBlur={this.onBlur(id, formContext)}
                                    hidden={hidden}/>
                            )}
                            {type && type.toLowerCase() === "number" && (
                                <NumberBox
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={formContext.values[id]}
                                    min={number && number.min}
                                    max={number && number.max}
                                    disabled={disabled || !formContext.isEditing}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id])}
                                    onBlur={this.onBlur(id, formContext)}/>
                            )}
                            {type && type.toLowerCase() === "date" && (
                                <TextBox
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={this.getDateStringFromTimestamp(formContext.values[id])}
                                    disabled={disabled || !formContext.isEditing}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id])}
                                    onBlur={this.onBlur(id, formContext)}/>
                            )}
                            {(type && type.toLowerCase() === "datepicker") && (
                                <Datepicker
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={formContext.values[id]}
                                    disabled={disabled || !formContext.isEditing}
                                    onSelect={this.onSelect(id, formContext)}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id])}/>
                            )}
                            {(type && type.toLowerCase() === "timepicker") && (
                                <Timepicker
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={formContext.values[id]}
                                    disabled={disabled || !formContext?.isEditing}
                                    onSelect={this.onSelect(id, formContext)}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id])}/>
                            )}
                            {type && type.toLowerCase() === "multilinetext" && (
                                <MultilineTextBox
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={formContext.values[id]}
                                    disabled={disabled || !formContext.isEditing}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id])}
                                    onBlur={this.onBlur(id, formContext)}/>
                            )}
                            {type && type.toLowerCase() === "dropdown" && dropdown && (
                                <Dropdown
                                    className={getEditorClassname(formContext.errors, !formContext.isEditing, formContext.values[id])}
                                    id={id}
                                    name={id}
                                    value={formContext.values[id]}
                                    disabled={disabled || !formContext.isEditing}
                                    onChange={this.onChange(id, formContext, !!formContext.errors[id], true)}
                                    onBlur={this.onBlur(id, formContext)}
                                    dropdown={dropdown}/>
                            )}
                            {(type && type.toLowerCase() === "list") && (
                                <CheckboxList id={id}
                                              name={id}
                                              required={formContext.isRequired(id)}
                                              values={this.props.value}
                                              disabled={disabled || !formContext?.isEditing}
                                              onCheckList={this.onCheckList(formContext)}/>
                            )}
                            {(type && type.toLowerCase() === "map") && (
                                map?.loading
                                    ? <LoadingSpinner/>
                                    : <LocationMap
                                        onSelect={(map?.editable && formContext.isEditing) ? this.onSelectCoordinates(id, formContext) : undefined}
                                        onDeselect={(map?.editable && formContext.isEditing) ? this.onDeselectCoordinates(id, formContext) : undefined}
                                        onClear={(map?.editable && formContext.isEditing) ? this.onClearCoordinates(id, formContext) : undefined}
                                        locations={this.getMapFieldMarkers(formContext.values[id], map?.markers, map?.valueToMarkers)}
                                        marker={{size: 5, labeled: map?.labeled}} hover clickHighlight
                                        zoomable={!map?.editable || (map?.zoomable && !formContext.isEditing)}
                                        resizable/>
                            )}
                            {(type && type.toLowerCase() === "checkbox") && (
                                <Checkbox id={id}
                                          label={checkbox?.label}
                                          checked={formContext.values[id]}
                                          disabled={disabled || !formContext?.isEditing}
                                          onCheck={this.onCheck(formContext, checkbox?.checkCallback)}/>
                            )}
                            {getError(formContext.errors) && (
                                <span className="helper-text red-text darken-3">
                    {getError(formContext.errors)}
                  </span>
                            )}
                        </div>
                    )
                )}
            </FormContext.Consumer>
        )
    }

    private getMapFieldMarkers = (values: any, markers?: IMarker[], valueToMarkers?: (v: T[]) => IMarker[]) => {
        markers = markers || [];
        if (!values) {
            return markers;
        }
        if (!Array.isArray(values)) {
            values = [values];
        }
        if (valueToMarkers) {
            values = valueToMarkers(values);
        }
        return values.concat(markers);
    }

    private updateField = () =>
        M.updateTextFields();

    private onChange = (id: string, formContext: IFormContext, validate: boolean, selected?: boolean) => (e: React.FormEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>): void => {
        const target = e.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;
        let value: string | number = target.value;
        // regex to test if value has only \n chars
        if (!new RegExp("^[\n\s]+$").test(value) && !isNaN(+value)) {
            value = `"${value}"`;
        }
        try {
            value = JSON.parse(value);
        } catch (_) {
        }
        if (getTypeFromValue(value) === 'number') {
            value = Number(value)
        }
        if (selected) {
            this.props.dropdown?.selectCallback?.(value);
        }
        formContext.setValue(id, value, validate);
    };

    private onSelect = (id: string, formContext: IFormContext) => (value?: string) => {
        formContext.setValue(id, value);
    };

    private onBlur = (id: string, formContext: IFormContext) => (): void =>
        formContext.validate(id);

    private onCheckList = (formContext: IFormContext) => (listId: keyof IValues, itemId: string, checked: boolean) => {
        if (checked) {
            formContext.addValue(listId, itemId);
        } else {
            formContext.removeValue(listId, itemId);
        }
    };

    private onCheck = (formContext: IFormContext, checkCallback?: (checked: boolean) => void) => (id: keyof IValues, checked: boolean) => {
        formContext.setValue(id, checked);
        checkCallback?.(checked);
    }

    private onSelectCoordinates = (id: string, formContext: any) => (marker: IMarker): void => {
        if (this.props.map?.singleMarker) {
            formContext.setValue(id, marker, false);
        } else {
            formContext.addValue(id, marker);
        }
    }

    private onDeselectCoordinates = (id: string, formContext: any) => (marker: IMarker): void =>
        formContext.removeValue(id, marker);

    private onClearCoordinates = (id: string, formContext: any) => (): void =>
        formContext.removeValuesExcept(id, this.props.map?.markers);

    private getDateStringFromTimestamp = (value: number) => {
        const date = new Date(value * 1000);
        return `${date.toLocaleDateString("pt")} ${date.toLocaleTimeString("pt")}`
    };

}