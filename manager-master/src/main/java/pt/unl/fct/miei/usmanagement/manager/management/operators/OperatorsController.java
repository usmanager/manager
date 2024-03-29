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

package pt.unl.fct.miei.usmanagement.manager.management.operators;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.unl.fct.miei.usmanagement.manager.operators.Operator;
import pt.unl.fct.miei.usmanagement.manager.rulesystem.condition.Condition;
import pt.unl.fct.miei.usmanagement.manager.services.operators.OperatorsService;
import pt.unl.fct.miei.usmanagement.manager.util.validate.Validation;

import java.util.List;

@RestController
@RequestMapping("/operators")
public class OperatorsController {

	private final OperatorsService operatorsService;

	public OperatorsController(OperatorsService operatorsService) {
		this.operatorsService = operatorsService;
	}

	@GetMapping
	public List<Operator> getOperators() {
		return operatorsService.getOperators();
	}

	@GetMapping("/{operatorName}")
	public Operator getOperator(@PathVariable String operatorName) {
		return operatorsService.getOperator(operatorName);
	}

	@PostMapping
	public Operator addOperator(@RequestBody Operator operator) {
		Validation.validatePostRequest(operator.getId());
		return operatorsService.addOperator(operator);
	}

	@PutMapping("/{operatorName}")
	public Operator updateOperator(@PathVariable String operatorName, @RequestBody Operator operator) {
		Validation.validatePutRequest(operator.getId());
		return operatorsService.updateOperator(operatorName, operator);
	}

	@DeleteMapping("/{operatorName}")
	public void deleteOperator(@PathVariable String operatorName) {
		operatorsService.deleteOperator(operatorName);
	}

	@GetMapping("/{operatorName}/conditions")
	public List<Condition> getConditions(@PathVariable String operatorName) {
		return operatorsService.getConditions(operatorName);
	}

}