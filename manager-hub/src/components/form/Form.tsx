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

import React, {createRef, RefObject} from "react";
import {deleteCancelRequest, deleteData, getCancelRequest, postData, putData} from "../../utils/api";
import styles from './Form.module.css';
import {RouteComponentProps, withRouter} from "react-router";
import {FieldProps, getTypeFromValue, IValidation} from "./Field";
import {camelCaseToSentenceCase, decodeHTML} from "../../utils/text";
import ConfirmDialog from "../dialogs/ConfirmDialog";
import {isEqual, isEqualWith} from "lodash";
import ActionProgressBar from "../actionloading/ActionProgressBar";
import PerfectScrollbar from "react-perfect-scrollbar";
import ScrollBar from "react-perfect-scrollbar";
import M from "materialize-css";
import {Method} from "axios";
import {ReduxState} from "../../reducers";
import {connect} from "react-redux";
import BaseComponent from "../BaseComponent";
import ReactTooltip from "react-tooltip";
import {GoMarkGithub} from "react-icons/all";

export interface IFormModal {
    id: string,
    title?: string,
    fields: IFields,
    values: IValues,
    position?: string,
    content: () => JSX.Element,
    fullScreen?: boolean,
    scrollbar?: React.RefObject<ScrollBar>,
    onResize?: () => void,
}

export type RestOperation = {
    textButton?: string,
    confirmMessage?: string,
    url: string,
    successCallback: (reply?: any, args?: any) => void,
    failureCallback: (reason: string, args?: any) => void,
    result?: (reply: any) => any;
}

export type IFields = {
    [key: string]: FieldProps;
}

export type IValues = {
    [key: string]: any;
}

export type IErrors = {
    [key: string]: string;
}

export type IFormDropdown = { id: string, title: string, empty: string, data: string[] };

export type ICustomButton = { button: JSX.Element, confirm?: { id: string, message: string, onClickConfirm: () => void } };

export type ISwitchDropdown = { title?: string, options: string[], onSwitch: (selected: any) => void };

export type IFormLoading = { method: Method, url: string } | undefined;

interface FormProps {
    id: string;
    fields: IFields;
    values: IValues;
    isNew?: boolean;
    showSaveButton?: boolean;
    post?: RestOperation;
    put?: RestOperation;
    delete?: RestOperation;
    controlsMode?: 'top' | 'modal' | 'modal-fullscreen' | 'none';
    modal?: {
        onConfirm?: (values: IValues) => void;
        scrollbar?: RefObject<ScrollBar>;
        scrollMaxHeight: number;
    }
    dropdown?: IFormDropdown;
    saveEntities?: (args: any) => void;
    customButtons?: ICustomButton[];
    switchDropdown?: ISwitchDropdown;
    loading?: IFormLoading;
    href?: string;
}

interface StateToProps {
    confirmationDialog: boolean,
}

type Props = FormProps & RouteComponentProps & StateToProps;

interface State {
    values: IValues;
    savedValues: IValues;
    errors: IErrors;
    isEditing: boolean;
    isValid: boolean;
    saveRequired: boolean;
    loading?: { method: Method, url: string };
}

export interface IFormContext {
    values: IValues;
    errors: IErrors;
    isEditing: boolean;
    setValue: (id: keyof IValues, value: any, validate?: boolean) => void;
    addValue: (id: keyof IValues, value: any) => void;
    removeValue: (id: keyof IValues, value: any) => void;
    removeValuesExcept: (id: keyof IValues, value: any[]) => void;
    validate: (id: keyof IValues) => void;
    isRequired: (id: keyof IValues) => boolean;
}

export const FormContext =
    React.createContext<IFormContext | null>(null);

//TODO validation for correct dates and times

export const requireGreaterOrEqualSize = (values: IValues, id: keyof IValues, size: number): string => {
    return values[id] === undefined || values[id] === null || values[id].length < size
        ? `O número de ${camelCaseToSentenceCase(id as string)} deve ser pelo menos ${size}`
        : "";
}

export const requiredSize = (values: IValues, id: keyof IValues, size: number): string => {
    return values[id] === undefined || values[id] === null || values[id].length != size
        ? `O número de ${camelCaseToSentenceCase(id as string)} deve ser exatamente ${size}`
        : "";
}

export const required = (values: IValues, id: keyof IValues): string => {
    return values[id] === undefined || values[id] === null || values[id] === ""
        ? `${camelCaseToSentenceCase(id as string)} é obrigatório`
        : "";
}

export const notAllowed = (values: IValues, id: keyof IValues, args: any[]): string =>
    args.indexOf(values[id].toLowerCase()) !== -1
        ? `${values[id]} não é permitido`
        : "";

export const notValid = (values: IValues, id: keyof IValues, regularExpression: RegExp, name: string): string =>
    !regularExpression.test(String(values[id]).toLowerCase())
        ? `${values[id]} não é um válido ${name}`
        : "";

export const notValidIpAddress = (values: IValues, id: keyof IValues): string =>
    notValid(values, id,
        /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        'endereço ip');

export const notValidEmail = (values: IValues, id: keyof IValues): string =>
    notValid(values, id,
        /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3})|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
        'email');

export const lengthLimit = (values: IValues, id: keyof IValues, size: number): string =>
    values[id].length > size
        ? `Máximo de ${size} caracteres`
        : "";

export const trimmed = (values: IValues, id: keyof IValues): string =>
    typeof values[id] === 'string' && (values[id].startsWith(" ") || values[id].endsWith(" "))
        ? `${camelCaseToSentenceCase(id as string)} não pode começar nem acabar com espaços`
        : "";

export const min = (values: IValues, id: keyof IValues, args: number): string =>
    values[id] < args
        ? `Mínimo de ${args}`
        : "";

export const max = (values: IValues, id: keyof IValues, args: number): string =>
    values[id] > args
        ? `Máximo de ${args}`
        : "";

export const number = (values: IValues, id: keyof IValues): string =>
    getTypeFromValue(values[id]) !== 'number'
        ? `${camelCaseToSentenceCase(id as string)} deve ser um número`
        : "";

export const maxSize = (values: IValues, id: keyof IValues, args: any) =>
    values[id] && values[id].length >= args
        ? `Máximo de ${args} caracteres`
        : "";

export const maxSizeAndTrimmed = (values: IValues, id: keyof IValues, args: any) =>
    trimmed(values, id) || maxSize(values, id, args);

export const requiredAndTrimmed = (values: IValues, id: keyof IValues) =>
    required(values, id) || trimmed(values, id);

export const requiredAndTrimmedAndNotValid = (values: IValues, id: keyof IValues, args: any[]) =>
    required(values, id) || trimmed(values, id) || notValid(values, id, args[0], args[1]);

export const requiredAndTrimmedAndNotValidIpAddress = (values: IValues, id: keyof IValues) =>
    required(values, id) || trimmed(values, id) || notValidIpAddress(values, id);

export const requiredAndTrimmedAndNotValidEmail = (values: IValues, id: keyof IValues) =>
    required(values, id) || trimmed(values, id) || notValidEmail(values, id);

export const requiredAndTrimmedAndNotAllowed = (values: IValues, id: keyof IValues, args: any[]) =>
    required(values, id) || trimmed(values, id) || notAllowed(values, id, args);

export const requiredAndTrimmedAndSizeRestriction = (values: IValues, id: keyof IValues, size: number) =>
    required(values, id) || trimmed(values, id) || lengthLimit(values, id, size);

export const requiredAndNumberAndMin = (values: IValues, id: keyof IValues, args: any) =>
    required(values, id) || number(values, id) || min(values, id, args);

export const requiredAndNumberAndMax = (values: IValues, id: keyof IValues, args: any) =>
    required(values, id) || number(values, id) || max(values, id, args);

export const requiredAndNumberAndMinAndMax = (values: IValues, id: keyof IValues, args: any) =>
    required(values, id) || number(values, id) || min(values, id, args[0]) || max(values, id, args[1]);

class Form extends BaseComponent<Props, State> {

    private mounted = false;
    private dropdown = createRef<HTMLButtonElement>();
    private dropdownScrollbar: (ScrollBar | null) = null;

    constructor(props: Props) {
        super(props);
        this.state = {
            values: props.values,
            savedValues: props.values,
            errors: {},
            isEditing: props.isNew || false,
            saveRequired: false,
            loading: props.loading,
            isValid: true,
        }
    }

    public componentDidMount(): void {
        this.initDropdown();
        this.mounted = true;
    }

    public componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any): void {
        if (prevProps.values !== this.props.values) {
            this.setState(Object.keys(this.props.values).reduce((state: State, data: string) => {
                if (!!this.props.values[data]) {
                    state.values[data] = this.props.values[data];
                }
                return state;
            }, this.state), () => {
                this.setState({saveRequired: this.props.showSaveButton || this.props.isNew || this.saveRequired()})
            });
        }
        if (prevProps.showSaveButton !== this.props.showSaveButton
            || prevState.values !== this.state.values
            || prevState.savedValues !== this.state.savedValues) {
            this.setState({saveRequired: this.props.showSaveButton || this.props.isNew || false})
        }
        if (prevState.values !== this.state.values || prevState.savedValues !== this.state.savedValues) {
            this.setState({saveRequired: this.saveRequired()})
        }
        if (prevProps.loading !== this.props.loading) {
            this.setState({loading: this.props.loading});
        }
        if (prevProps.isNew !== this.props.isNew) {
            this.setState({isEditing: this.props.isNew === undefined || this.props.isNew, saveRequired: false});
        }
        this.initDropdown();
    }

    public componentWillUnmount(): void {
        this.mounted = false;
    }

    public render() {
        const context: IFormContext = {
            values: this.state.values,
            errors: this.state.errors,
            isEditing: this.state.isEditing,
            setValue: this.setValue,
            addValue: this.addValue,
            removeValue: this.removeValue,
            removeValuesExcept: this.removeValuesExcept,
            validate: this.validate,
            isRequired: this.isRequired,
        };
        const {saveRequired, loading} = this.state;
        const {
            id, isNew, values, controlsMode, put: editable, delete: deletable, customButtons, dropdown, switchDropdown,
            children, confirmationDialog, href
        } = this.props;
        return (
            <>
                <ReactTooltip id='tooltip' effect='solid' type='light'/>
                <ReactTooltip id='dark-tooltip' effect='solid' type='dark'/>
                {this.props.delete && confirmationDialog && (
                    <ConfirmDialog id={'confirm-delete'}
                                   message={`${this.props.delete.confirmMessage || `${this.props.delete.textButton?.toLowerCase() || 'apagar'} ${values[id]}`}`}
                                   confirmCallback={this.onClickDelete}/>)}
                <form onSubmit={this.handleSubmit} noValidate>
                    {(controlsMode === undefined || controlsMode === 'top') && (
                        <div>
                            <div className='controlsContainer noBorder'>
                                {switchDropdown && (
                                    <>
                                        <button
                                            className={`dropdown-trigger btn-floating btn-flat btn-small right`}
                                            data-for='dark-tooltip'
                                            data-tip={switchDropdown.title || 'Alterar formulário'} data-place='bottom'
                                            data-target={`switch-dropdown`}
                                            type={'button'}
                                            ref={this.dropdown}>
                                            <i className='material-icons'>keyboard_arrow_down</i>
                                        </button>
                                        <ul id='switch-dropdown'
                                            className={`dropdown-content ${styles.formDropdown} form-dropdown`}>
                                            <li className={`${styles.disabled}`}>
                                                <a>
                                                    {switchDropdown.title || 'Selecionar o tipo de formulário'}
                                                </a>
                                            </li>
                                            <PerfectScrollbar ref={(ref) => {
                                                this.dropdownScrollbar = ref;
                                            }}>
                                                {switchDropdown.options.map((option, index) =>
                                                    <li key={index} onClick={this.switchForm}>
                                                        <a>
                                                            {option}
                                                        </a>
                                                    </li>
                                                )}
                                            </PerfectScrollbar>
                                        </ul>
                                    </>
                                )}
                                {isNew && !loading
                                    ?
                                    <button
                                        className={`${styles.controlButton} btn-flat btn-small green-text left slide`}>
                                        {this.props.post?.textButton || 'Guardar'}
                                    </button>
                                    :
                                    <>
                                        <div className={`${styles.controlButton}`}>
                                            {!loading && customButtons?.map((button, index) => (
                                                <div key={index} className={styles.customButton}>
                                                    {button.confirm && (
                                                        <ConfirmDialog key={button.confirm.id}
                                                                       id={button.confirm.id}
                                                                       message={button.confirm?.message}
                                                                       confirmCallback={button.confirm?.onClickConfirm}/>)}
                                                    {button.button}
                                                </div>
                                            ))}
                                            {deletable !== undefined && !loading && (
                                                <button
                                                    className={`${confirmationDialog ? 'modal-trigger' : ''} btn-flat btn-small red-text inline-button`}
                                                    type='button'
                                                    data-target='confirm-delete'
                                                    onClick={confirmationDialog ? undefined : this.onClickDelete}>
                                                    {this.props.delete?.textButton || 'Apagar'}
                                                </button>)}
                                            {!loading && (
                                                <button
                                                    className={`btn-flat btn-small green-text slide inline-button`}
                                                    /*style={saveRequired ? {transform: "scale(1)"} : {transform: "scale(0)"}}*/
                                                    style={editable === undefined && !saveRequired ? {visibility: 'hidden'} : undefined}
                                                    disabled={!saveRequired}>
                                                    {(editable !== undefined && this.props.post?.textButton) || 'Guardar'}
                                                </button>)}
                                        </div>
                                        {editable !== undefined && !loading && (
                                            <button
                                                className={`btn-floating btn-flat btn-small right inline-button`}
                                                data-for='dark-tooltip' data-tip='Editar' data-place='bottom'
                                                type='button'
                                                onClick={this.onClickEdit}>
                                                <i className={`large material-icons ${this.state.isEditing ? (this.state.isValid ? 'green-text' : 'red-text') : ''}`}>edit</i>
                                            </button>
                                        )}
                                        {href && <a className='right' href={href} target='_blank' rel='noopener noreferrer'>
                                            <i style={{marginTop: "4px"}} className='material-icons'>link</i>
                                        </a>}
                                    </>
                                }
                                {loading && (
                                    <button
                                        className={`${styles.controlButton} btn-flat btn-small red-text right slide inline-button`}
                                        type='button'
                                        onClick={this.cancelRequest}>
                                        Cancelar
                                    </button>
                                )}
                                {dropdown && (
                                    <>
                                        <button
                                            className={`dropdown-trigger btn-floating btn-flat btn-small right inline-button`}
                                            data-for='dark-tooltip' data-tip={dropdown.title} data-place='bottom'
                                            data-target={`dropdown-${dropdown.id}`}
                                            type='button'
                                            ref={this.dropdown}>
                                            <i className='material-icons'>add</i>
                                        </button>
                                        <ul id={`dropdown-${dropdown.id}`}
                                            className={`dropdown-content ${styles.formDropdown}`}>
                                            <li className={`${styles.disabled}`}>
                                                <a className={`${!dropdown?.data.length ? styles.dropdownEmpty : ''}`}>
                                                    {dropdown.data.length ? dropdown.title : dropdown.empty}
                                                </a>
                                            </li>
                                            <PerfectScrollbar ref={(ref) => {
                                                this.dropdownScrollbar = ref;
                                            }}>
                                                {dropdown.data.map((data, index) =>
                                                    <li key={index} onClick={this.onAdd}>
                                                        <a>
                                                            {data}
                                                        </a>
                                                    </li>
                                                )}
                                            </PerfectScrollbar>
                                        </ul>
                                    </>
                                )}
                            </div>
                            <ActionProgressBar loading={!!loading} backgroundColor={!!loading ? 'transparent' : 'black'} progressBarColor={"black"}/>
                        </div>
                    )}
                    {controlsMode?.includes('modal') ?
                        <ScrollBar ref={this.props.modal?.scrollbar}
                                   component={'div'}
                                   style={this.props.modal?.scrollMaxHeight
                                       ? {maxHeight: Math.floor(this.props.modal?.scrollMaxHeight)}
                                       : undefined}
                                   options={{
                                       useBothWheelAxes: false,
                                       suppressScrollX: true
                                   }}>
                            <div
                                className={`${React.Children.count(children) === 0 ? styles.emptyContent : styles.content}`}>
                                <FormContext.Provider value={context}>
                                    {children}
                                </FormContext.Provider>
                            </div>
                        </ScrollBar>
                        : <div
                            className={`${React.Children.count(children) === 0 ? styles.emptyContent : styles.content}`}>
                            <FormContext.Provider value={context}>
                                {children}
                            </FormContext.Provider>
                        </div>
                    }
                    {(controlsMode === 'modal' || controlsMode === 'modal-fullscreen') && (
                        <div
                            className={`modal-footer dialog-footer ${controlsMode === 'modal-fullscreen' ? 'modal-footer-fullscreen' : ''}`}>
                            <div>
                                <button className={`btn-flat red-text inline-button`}
                                        type='button'
                                        onClick={this.clearValues}>
                                    Apagar
                                </button>
                                <button
                                    className={`modal-close btn-flat red-text inline-button`}
                                    type='button'>
                                    Cancelar
                                </button>
                                <button className={`btn-flat green-text inline-button`}
                                        type='button'
                                        onClick={this.onModalConfirm}>
                                    Confirmar
                                </button>
                            </div>
                        </div>
                    )}
                </form>
            </>
        )
    }

    private initDropdown = () =>
        M.Dropdown.init(this.dropdown.current as Element,
            {
                onOpenEnd: this.onOpenDropdown
            });

    private onOpenDropdown = () =>
        this.dropdownScrollbar?.updateScroll();

    private saveRequired = () => {
        return !isEqualWith(this.state.savedValues, this.state.values, (first, second) =>
            ((typeof first === 'boolean' && typeof second === 'string' && first.toString() === second)
                || (typeof first === 'string' && typeof second === 'boolean' && first === second.toString())
                || (typeof first === 'number' && typeof second === 'string' && first.toString() === second)
                || (typeof first === 'string' && typeof second === 'number' && first === second.toString())) || undefined);
    };

    private isRequired = (id: keyof IValues): boolean => {
        const {fields} = this.props;
        const field: FieldProps | undefined = fields[id];
        const validation: IValidation | boolean | undefined = field?.validation;
        return typeof validation === 'object' && validation?.rule.name.includes('required') || false
    }

    private validate = (id: keyof IValues): string => {
        const {fields} = this.props;
        const field: FieldProps | undefined = fields[id];
        const validation: IValidation | boolean | undefined = field?.validation;
        const newError: string = typeof validation === 'object' && validation ? validation.rule(this.state.values, id, validation.args) : "";
        if (newError !== "") {
            console.log("Validation of field " + field.id + ": " + newError);
        }
        this.setState(_ => ({errors: {...this.state.errors, [id]: newError}, isValid: newError === ""}));
        return newError;
    };

    private validateForm(setErrors: boolean): boolean {
        const errors: IErrors = {};
        Object.entries(this.props.fields).forEach(([fieldName, field]) => {
            const validateMessage = this.validate(fieldName);
            if (field.hidden && validateMessage.length) {
                super.toast(`<div class='red-text'>Validation failed:</div> ${validateMessage}`);
            }
            errors[fieldName] = validateMessage;
        });
        if (setErrors) {
            this.setState({errors});
        }
        return this.isValid(errors);
    }

    private isValid = (errors: IErrors) => {
        const isValid = Object.values(errors).every(error => !error);
        this.setState(_ => ({isValid}));
        return isValid;
    }


    private onClickEdit = (): void => {
        this.setState(prevState => ({isEditing: !prevState.isEditing}));
    };

    private onClickDelete = (): void => {
        if (this.props.delete) {
            const args = this.state.values;
            deleteData(this.props.delete.url,
                () => {
                    this.props.delete?.successCallback(args);
                    if (this.mounted) {
                        this.setState({loading: undefined});
                    }
                },
                (reply) => {
                    this.props.delete?.failureCallback(reply, args);
                    if (this.mounted) {
                        this.setState({loading: undefined});
                    }
                });
            this.setState({loading: {method: 'delete', url: this.props.delete.url}, isEditing: false});
        }
    };

    private handleSubmit = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        const validate = this.validateForm(true);
        const {isNew, post, put, saveEntities} = this.props;
        const args = this.state.values;
        let requestBody = {};
        // replace trailing \n
        for (const key in args) {
            requestBody = {
                ...requestBody,
                [key]: !args || typeof args[key] !== 'string' ? args[key] : args[key].replace(/\n+$/, "")
            }
        }
        if (isNew) {
            if (post?.url) {
                if (validate) {
                    postData(post.url, requestBody,
                        (reply) => {
                            post.successCallback(reply, requestBody);
                            if (this.mounted) {
                                const data = post.result?.(reply.data) || reply.data;
                                this.setState({
                                    values: data as IValues,
                                    savedValues: data as IValues,
                                    loading: undefined
                                });
                            }
                        },
                        (reply) => {
                            post.failureCallback(reply, requestBody);
                            if (this.mounted) {
                                this.setState({loading: undefined});
                            }
                        });
                    this.setState({loading: {method: 'post', url: post.url}});
                }
            }
        } else {
            const saveRequired = this.saveRequired();
            if (saveRequired) {
                if (put?.url && validate) {
                    putData(put.url, requestBody,
                        (reply) => {
                            put.successCallback(reply, requestBody);
                            if (this.mounted) {
                                const data = put.result?.(reply.data) || reply.data;
                                this.setState({
                                    values: data as IValues,
                                    savedValues: data as IValues,
                                    loading: undefined
                                });
                            }
                        },
                        (reason) => {
                            put.failureCallback(reason, requestBody);
                            if (this.mounted) {
                                this.setState({loading: undefined});
                            }
                        });
                    this.setState({loading: {method: 'put', url: put.url}});
                }
            } else {
                saveEntities?.(requestBody);
            }
        }
    };

    private cancelRequest = () => {
        const {loading} = this.state;
        if (loading?.method && loading.url) {
            getCancelRequest(loading.method, loading.url)?.cancel('Operation canceled by the user');
            deleteCancelRequest(loading.method, loading.url);
            this.setState({loading: undefined});
        }
    };

    private setValue = (id: keyof IValues, value: IValues, validate?: boolean) => {
        if (validate === undefined || validate) {
            this.setState(state => ({values: {...state.values, [id]: value}}), () => this.validate(id as string));
        } else {
            this.setState(state => ({values: {...state.values, [id]: value}}));
        }
    };

    private addValue = (id: keyof IValues, value: any) => {
        this.setState(state => {
            let values = state.values[id] ? [...state.values[id], value] : [value];
            return {values: {...state.values, [id]: values}}
        });
    }

    private removeValuesExcept = (id: keyof IValues, values?: any[]) => {
        this.setState(state => {
            let newValues: undefined | any[] = undefined;
            if (values && Array.isArray(state.values[id])) {
                newValues = state.values[id]?.filter((v: any) => values.includes(v))
                if (!newValues?.length) {
                    newValues = undefined;
                }
            }
            return {values: {...state.values, [id]: newValues}};
        });
    }

    private removeValue = (id: keyof IValues, value?: any) => {
        this.setState(state => {
            let values = undefined;
            if (value && Array.isArray(state.values[id])) {
                values = state.values[id]?.filter((v: any) => !isEqual(v, value));
                if (!values.length) {
                    values = undefined;
                }
            }
            return {values: {...state.values, [id]: values}};
        });
    };

    private onModalConfirm = (event: React.FormEvent<HTMLButtonElement>) => {
        event.preventDefault();
        if (!this.validateForm(true)) {
            return;
        }
        this.props.modal?.onConfirm?.(this.state.values);
        this.clearValues();
    };

    private clearValues = () =>
        this.setState({values: this.props.values, errors: {}});

    private onAdd = (event: React.MouseEvent<HTMLLIElement>): void => {
        const data = decodeHTML((event.target as HTMLLIElement).innerHTML);
        // @ts-ignore
        this.setState({[data]: {value: data, isChecked: false, isNew: true}});
    };

    private switchForm = (e: React.FormEvent<HTMLLIElement>) => {
        const selectedForm = decodeHTML((e.target as HTMLLIElement).innerHTML);
        this.props.switchDropdown?.onSwitch(selectedForm);
        this.clearValues();
    };

}

const mapStateToProps = (state: ReduxState): StateToProps => (
    {
        confirmationDialog: state.ui.confirmationDialog,
    }
);


export default withRouter(connect(mapStateToProps, undefined)(Form));
