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
import {connect} from "react-redux";
import {INewSshCommand, ISshCommand} from "../../ssh/SshCommand";
import SshPanel, {ICommand, IFileTransfer} from "../../ssh/SshPanel";
import {addCommand} from "../../../../actions";
import BaseComponent from "../../../../components/BaseComponent";
import Field from "../../../../components/form/Field";
import Form, {IFields, requiredAndTrimmed} from "../../../../components/form/Form";
import {IReply} from "../../../../utils/api";
import {IEdgeHost} from "./EdgeHost";
import {getEdgeHostAddress} from "../Hosts";

const buildNewSshCommand = (): INewSshCommand => ({
    background: false,
    hostAddress: undefined,
    command: undefined,
});

interface EdgeHostSshCommandProps {
    edgeHost: IEdgeHost;
}

interface DispatchToProps {
    addCommand: (command: ICommand) => void;
}

type Props = EdgeHostSshCommandProps & DispatchToProps;

class EdgeHostSshCommand extends BaseComponent<Props, {}> {

    private sshPanel: any = null;

    private commandFilter = (command: ICommand | IFileTransfer) =>
        command.hostAddress.publicIpAddress === this.props.edgeHost.publicIpAddress

    private onSshPanelRef = (ref: any) => this.sshPanel = ref;

    private scrollToBottom = () =>
        this.sshPanel.scrollTop = Number.MAX_SAFE_INTEGER;

    public render() {
        const command = buildNewSshCommand();
        return (
            <>
                {/*@ts-ignore*/}
                <Form id={'sshCommand'}
                      fields={this.getFields()}
                      values={command}
                      isNew
                      post={{
                          textButton: 'Executar',
                          url: `hosts/edge/${this.props.edgeHost?.publicIpAddress}/${this.props.edgeHost?.privateIpAddress}/ssh`,
                          successCallback: this.onPostSuccess,
                          failureCallback: this.onPostFailure
                      }}>
                    <Field key='background'
                           id='background'
                           type='checkbox'
                           checkbox={{label: 'executar em background'}}/>
                    <Field key='command'
                           id='command'
                           label='command'/>
                </Form>
                {this.props.edgeHost && <SshPanel onRef={this.onSshPanelRef}
                                                  filter={this.commandFilter}
                                                  hostAddress={getEdgeHostAddress(this.props.edgeHost)}/>}
            </>
        );
    }

    private onPostSuccess = (reply: IReply<ISshCommand>): void => {
        const command = reply.data;
        const edgeHost = this.props.edgeHost;
        const hostAddress = getEdgeHostAddress(edgeHost);
        const timestampedCommand = {...command, hostAddress: hostAddress, timestamp: Date.now()};
        this.props.addCommand(timestampedCommand);
        this.scrollToBottom();
    };

    private onPostFailure = (reason: string): void =>
        super.toast(`Command execution failed`, 10000, reason, true);

    private getFields = (): IFields => (
        {
            background: {
                id: 'background',
                label: 'background',
            },
            command: {
                id: 'command',
                label: 'command',
                validation: {rule: requiredAndTrimmed}
            },
        }
    );

}

const mapDispatchToProps: DispatchToProps = {
    addCommand,
};

export default connect(undefined, mapDispatchToProps)(EdgeHostSshCommand);
