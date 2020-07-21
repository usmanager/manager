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

import React from 'react';
import Sidenav from "../sidenav/Sidenav";
import {ReduxState} from "../../reducers";
import {connect} from "react-redux";
import Breadcrumbs from "../../components/breadcrumbs/Breadcrumbs";
import M from "materialize-css";

interface StateToProps {
  sidenavVisible: boolean;
}

type Props = StateToProps;

class MainLayout extends React.Component<Props, {}> {

  public componentDidMount(): void {
    M.AutoInit();
  }

  public render() {
    const paddingLeft = this.props.sidenavVisible ? 200 : 0;

    return (
      <div>
        <Sidenav/>
        <div className="section content" style={{paddingLeft, transition: 'padding-left .25s'}}>
          <div className="row col s12">
            {/*
            // @ts-ignore*/}
            <Breadcrumbs/>
          </div>
          <div className='row col s12 m12'>
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }

}

const mapStateToProps = (state: ReduxState): StateToProps => (
  {
    sidenavVisible: state.ui.sidenav.user && state.ui.sidenav.width,
  }
);

export default connect(mapStateToProps)(MainLayout);
