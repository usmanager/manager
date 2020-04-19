import React, {createRef} from "react";
import M from "materialize-css";
import List from "./List";
import styles from "./ControlledList.module.css";
import PerfectScrollbar from "react-perfect-scrollbar";
import BaseComponent from "../BaseComponent";
import {deleteData} from "../../utils/api";
import {decodeHTML} from "../../utils/text";
import InputDialog from "../dialogs/InputDialog";
import {IFields, IValues, RestOperation} from "../form/Form";
import ScrollBar from "react-perfect-scrollbar";

type FormModal = {
  id: string,
  title?: string,
  fields: IFields,
  values: IValues,
  position?: string,
  content: () => JSX.Element,
  onOpen?: (selected: any) => void,
  open?: boolean,
};

interface ControlledListProps<T> {
  dataKey?: string,
  isLoading: boolean;
  error?: string | null;
  emptyMessage: string;
  data: T[];
  dropdown?: { id: string, title: string, empty: string, data: string[], formModal?: FormModal};
  formModal?: FormModal;
  show: (index: number, element: T, separate: boolean, checked: boolean,
         handleCheckbox: (event: React.ChangeEvent<HTMLInputElement>) => void) => JSX.Element;
  onAdd?: (data: string) => void;
  onAddInput?: (input: IValues) => void;
  onRemove: (data: string[]) => void;
  onDelete: RestOperation;
}

type Props<T> = ControlledListProps<T>;

interface State<T> {
  [key: string]: { value: T, isChecked: boolean, isNew: boolean } | undefined;
}

export default class ControlledList<T> extends BaseComponent<Props<T>, State<T>> {

  private globalCheckbox = createRef<HTMLInputElement>();
  private dropdown = createRef<HTMLButtonElement>();
  private scrollbar: (ScrollBar | null) = null;
  private selected?: string;

  state: State<T> = {};

  componentDidMount(): void {
    this.init()
  }

  componentDidUpdate(prevProps: Readonly<Props<T>>, prevState: Readonly<State<T>>, snapshot?: any): void {
    if (this.globalCheckbox.current) {
      this.globalCheckbox.current.checked = Object.values(this.state)
                                                  .map(data => !data || data.isChecked)
                                                  .every(checked => checked);
    }
    if (prevProps.data !== this.props.data) {
      console.log(Object.values(this.props.data))
      this.setState(Object.values(this.props.data).reduce((state: State<T>, data: T) => {
        if (typeof data == 'string' || this.props.dataKey) {
          // @ts-ignore
          state[typeof data == 'string' ? data : data[this.props.dataKey]] = { value: data, isChecked: false, isNew: false };
        }
        return state;
      }, {}));
    }
    this.init();
  }

  private init = () =>
    M.Dropdown.init(this.dropdown.current as Element,
      {
        onOpenEnd: this.onOpenDropdown
      });

  private onOpenDropdown = () =>
    this.scrollbar?.updateScroll();

  private handleGlobalCheckbox = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {checked} = event.target;
    this.setState(state =>
      Object.entries(state).filter(([_, data]) => data).reduce((newState: State<T>, [data, dataState]) => {
        if (dataState) {
          newState[data] = { value: dataState.value, isChecked: checked, isNew: dataState.isNew};
        }
        return newState;
      }, {}));
  };

  private handleCheckbox = (event: React.ChangeEvent<HTMLInputElement>) => {
    const {id: data, checked} = event.target;
    if (this.state[data]) {
      // @ts-ignore
      this.setState(state => ({[data]: { value: state[data].value, isChecked: checked, isNew: state[data].isNew } }));
    }
  };

  private show = (data: T, index: number): JSX.Element => {
    /*const checked = this.state[data]?.isChecked || false;*/
    const separate = index != Object.entries(this.state).filter(([_, data]) => data).length - 1;
    return this.props.show(index, data, separate, true, this.handleCheckbox)
  };

  private onAdd = (event: React.MouseEvent<HTMLLIElement>): void => {
    const data = decodeHTML((event.target as HTMLLIElement).innerHTML);
    // @ts-ignore
    this.setState({ [data]: { value: data, isChecked: false, isNew: true } });
    this.props.onAdd?.(data);
  };

  private onAddDropdownModalInput = (input: any): void => {
    if (this.selected && this.props.dropdown?.formModal && this.props.dataKey) {
      input = { [this.props.dataKey]: this.selected, ...input };
      this.setState({ [this.selected]: { value: input, isChecked: false, isNew: true } });
      this.props.onAddInput?.(input);
    }
  };

  private onAddFormModalInput = (input: any): void => {
    const key = this.props.dataKey && input[this.props.dataKey];
    this.setState({ [key]: {value: input, isChecked: false, isNew: true } });
    this.props.onAddInput?.(input);
  };

  private onRemove = (): void => {
    const checkedData = Object.entries(this.state).filter(([_, data]) => data?.isChecked);
    const unpersistedData = checkedData.filter(([_, data]) => data?.isNew).map(([name, _]) => name);
    if (unpersistedData.length) {
      this.invalidateStateData(unpersistedData);
      this.props.onRemove(unpersistedData);
    }
    const persistedData = checkedData.filter(([_, data]) => !data?.isNew).map(([name, _]) => name);
    if (persistedData.length) {
      const {url} = this.props.onDelete;
      if (persistedData.length == 1) {
        deleteData(`${url}/${persistedData[0]}`, () => this.onDeleteSuccess(persistedData), this.onDeleteFailure);
      }
      else {
        deleteData(url, () => this.onDeleteSuccess(persistedData), this.onDeleteFailure, persistedData);
      }
    }
  };

  private onDeleteSuccess = (data: string[]): void => {
    this.invalidateStateData(data);
    this.props.onDelete.successCallback(data);
  };

  private invalidateStateData = (data: string[]): void =>
    this.setState(data.reduce((state: State<T>, data: string) => {
      state[data] = undefined;
      return state;
    }, {}));

  private onDeleteFailure = (reason: string): void =>
    this.props.onDelete.failureCallback(reason);

  private inputDialog = (formModal: FormModal, preSelected?: boolean): JSX.Element => {
    return <InputDialog id={formModal.id}
                        title={formModal.title}
                        fields={formModal.fields}
                        values={formModal.values}
                        position={formModal.position}
                        confirmCallback={preSelected ? this.onAddDropdownModalInput : this.onAddFormModalInput}
                        open={formModal.open}>
      {formModal?.content()}
    </InputDialog>;
  };

  private setSelected = (event: any) => {
    this.selected = decodeHTML((event.target as HTMLLIElement).innerHTML);
    this.props.dropdown?.formModal?.onOpen?.(this.selected);
  };

  render() {
    const {isLoading, error, emptyMessage, dropdown, formModal} = this.props;
    // @ts-ignore
    const data = Object.values(this.state).filter(data => data).map(data => data.value);
    const DataList = List<T>();
    return (
      <div>
        <div className={`controlsContainer`}>
          {data.length > 0 && (
            <p className={`${styles.nolabelCheckbox}`}>
              <label>
                <input type="checkbox"
                       onChange={this.handleGlobalCheckbox}
                       ref={this.globalCheckbox}/>
                <span/>
              </label>
            </p>
          )}
          {dropdown && (
            <>
              <button className={`dropdown-trigger btn-floating btn-flat btn-small waves-effect waves-light right tooltipped`}
                      data-position="bottom" data-tooltip={dropdown.title}
                      data-target={`dropdown-${dropdown.id}`}
                      ref={this.dropdown}>
                <i className="material-icons">add</i>
              </button>
              <ul id={`dropdown-${dropdown.id}`}
                  className={`dropdown-content ${styles.dropdown}`}>
                <li className={`${styles.disabled}`}>
                  <a className={`${!dropdown?.data.length ? styles.dropdownEmpty : undefined}`}>
                    {dropdown.data.length ? dropdown.title : dropdown.empty}
                  </a>
                </li>
                <PerfectScrollbar ref={(ref) => { this.scrollbar = ref; }}>
                  {dropdown.data.map((data, index) =>
                    <li key={index} onClick={!dropdown?.formModal ? this.onAdd : this.setSelected}>
                      <a>
                        {data}
                      </a>
                    </li>
                  )}
                </PerfectScrollbar>
              </ul>
              {console.log(dropdown?.formModal)}
              {dropdown?.formModal && this.inputDialog(dropdown?.formModal, true)}
            </>
          )}
          {(formModal &&
            <>
                <button className={`modal-trigger btn-floating btn-flat btn-small waves-effect waves-light right tooltipped`}
                        data-position="bottom" data-tooltip={formModal.title}
                        data-target={formModal.id}>
                    <i className="material-icons">add</i>
                </button>
              {this.inputDialog(formModal)}
            </>
          )}
          <button className="btn-flat btn-small waves-effect waves-light red-text right"
                  style={Object.values(this.state)
                               .map(dependency => dependency?.isChecked || false)
                               .some(checked => checked)
                    ? {transform: "scale(1)"}
                    : {transform: "scale(0)"}}
                  onClick={this.onRemove}>
            Remove
          </button>
        </div>
        <DataList
          isLoading={isLoading}
          error={error}
          emptyMessage={emptyMessage}
          list={data}
          show={this.show}/>
      </div>
    )
  }

}