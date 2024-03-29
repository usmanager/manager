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

import List from "../../../components/list/List";
import React from "react";
import {ReduxState} from "../../../reducers";
import {loadLogs} from "../../../actions";
import {connect} from "react-redux";
import {ILogs} from "./ManagementLogs";
import ListItem from "../../../components/list/ListItem";
import styles from './LogsList.module.css';
import ActionButton from "../../../components/list/ActionButton";
import {capitalize} from "../../../utils/text";

interface StateToProps {
    isLoading: boolean;
    error?: string | null;
    logs: ILogs[];
}

interface DispatchToProps {
    loadLogs: () => void;
}

type Props = StateToProps & DispatchToProps;

class LogsList extends React.Component<Props, {}> {

    private reloadLogs: NodeJS.Timeout | null = null;

    public componentDidMount(): void {
        this.props.loadLogs();
    };

    public componentWillUnmount(): void {
        if (this.reloadLogs) {
            clearTimeout(this.reloadLogs);
            this.reloadLogs = null;
        }
    }

    public render() {
        const {error, logs} = this.props;
        let isLoading = this.props.isLoading;
        const LogsList = List<ILogs>();
        return (
            <>
                <ActionButton icon={'cached'}
                              tooltip={{
                                  activatedText: 'Desativar carregamento periódico',
                                  deactivatedText: 'Ativar carregamento periódico a cada 15 segundos',
                                  position: 'bottom'
                              }}
                              clickCallback={this.onReloadClick}
                              automatic/>
                <div className={`${styles.container} ${!isLoading && !error ? styles.list : undefined}`}>
                    <LogsList
                        isLoading={!this.reloadLogs && isLoading}
                        error={error}
                        emptyMessage={'Não há logs para mostrar'}
                        list={logs}
                        show={this.log}
                        paginate={{
                            pagesize: {initial: 25, options: [5, 10, 25, 50, 100, 'Tudo']},
                            page: {last: true},
                            position: 'top-bottom'
                        }}
                        predicate={this.predicate}
                        header={this.header}/>
                </div>
            </>
        );
    }

    private predicate = (logs: ILogs, search: string): boolean =>
        logs.formattedMessage.toLowerCase().includes(search)
        || logs.levelString.toLowerCase().includes(search);

    private getLevelColor = (levelString: string) => {
        switch (levelString.toLowerCase()) {
            case 'trace':
                return 'grey-text';
            case 'debug':
                return 'green-text';
            case 'info':
                return 'blue-text';
            case 'warn':
                return 'orange-text';
            case 'error':
                return 'red-text';
        }
    };

    private getLevelMargin = (levelString: string): string => {
        switch (levelString.toLowerCase()) {
            case 'trace':
                return '0 7px 0 7px';
            case 'debug':
                return '0 0 0 7px';
            case 'info':
                return '0 17px 0 7px';
            case 'warn':
                return '0 9px 0 7px';
            case 'error':
                return '0 9px 0 7px';
        }
        return '0px';
    }

    private header = (): JSX.Element =>
        <ListItem>
            <div className={`logs-header-item`}>
                <span className={`${styles.timestampColumn}`}>
                    timestamp
                </span>
                <span className={`${styles.levelColumn}`}>
                    nível
                </span>
                <span className={`${styles.infoColumn}`}>
                    mensagem
                </span>
            </div>
        </ListItem>;

    private log = (log: ILogs, index: number, last: boolean): JSX.Element =>
        <ListItem>
            <div className={`${last ? 'logs-last-item' : 'logs-item'}`}>
        <span className={`${styles.column}`}>
          {new Date(log.timestmp).toLocaleString()}
        </span>
                <span className={`${this.getLevelColor(log.levelString)}`}
                      style={{margin: this.getLevelMargin(log.levelString)}}>
          {capitalize(log.levelString.toLowerCase())}
        </span>
                <span className={`${styles.column}`}>
          {log.formattedMessage}
        </span>
            </div>
        </ListItem>;

    private onReloadClick = (): void => {
        if (this.reloadLogs) {
            clearTimeout(this.reloadLogs);
            this.reloadLogs = null;
        } else {
            this.reloadLogs = setInterval(this.props.loadLogs, 5000);
        }
    };

}

const mapStateToProps = (state: ReduxState): StateToProps => (
    {
        isLoading: state.entities.logs.isLoadingLogs,
        error: state.entities.logs.loadLogsError,
        logs: (state.entities.logs && Object.values(state.entities.logs.data)) || [],
    }
);

const mapDispatchToProps: DispatchToProps = {
    loadLogs
};

export default connect(mapStateToProps, mapDispatchToProps)(LogsList)