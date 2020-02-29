import React, {createRef} from "react";
import {RouteComponentProps} from "react-router";
import M from "materialize-css";
import styles from './Tabs.module.css';

export type Tab = { title: string, id: string, content: () => JSX.Element }

interface TabsProps {
  tabs: Tab[];
}

type Props = TabsProps & RouteComponentProps;

export default class extends React.Component<Props, {}> {

  private tabs = createRef<HTMLUListElement>();

  componentDidMount(): void {
    M.Tabs.init(this.tabs.current as Element);
  }

  updatePathname = (event: any) => {
    this.props.history.push(`#${event.target.id}`);
  };

  render() {
    const {tabs} = this.props;
    return (
      <>
        <ul className="tabs" ref={this.tabs}>
          {tabs.map((tab, index) =>
            <li key={index} className={`tab col s${Math.floor(12/tabs.length)}`} onClick={this.updatePathname}>
              <a href={tabs.length == 1 ? undefined : `#${tab.id}`}>{tab.title}</a>
            </li>
          )}
        </ul>
        {tabs.map((tab, index) =>
          <div  id={tab.id} key={index} className={`tab-content ${styles.tabContent} col s12`}>
            {tab.content()}
          </div>
        )}
      </>
    )
  }
}