/*
 * Copyright (c) 2020 usmanager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import React from "react";
import MainLayout from "../../../../views/mainLayout/MainLayout";
import AddButton from "../../../../components/form/AddButton";
import styles from './RulesContainer.module.css';
import RulesContainerList from "./RulesContainerList";

const RulesContainer: React.FC = () =>
  <MainLayout>
    <AddButton tooltip={{text: 'Add container rule', position: 'left'}}
               pathname={'/rules/containers/new_container_rule?new=true'}/>
    <div className={`${styles.container}`}>
      <RulesContainerList/>
    </div>
  </MainLayout>;

export default RulesContainer;
