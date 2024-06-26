// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.util.function.Supplier;

import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ShooterConstants;
import frc.robot.Constants.FeederConstants;


public class Feeder extends SubsystemBase {
  private CANSparkFlex m_FeederMotor;
  /** Creates a new Feeder. */
  public Feeder() {
    m_FeederMotor = new CANSparkFlex(FeederConstants.kFeederCANID, MotorType.kBrushless);
    m_FeederMotor.restoreFactoryDefaults();
    m_FeederMotor.setSmartCurrentLimit(FeederConstants.kFeederCurrentLimit);
    
    m_FeederMotor.burnFlash();
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run  
  }

  public void runMotors(Supplier<Double> speedSupplier) {
     m_FeederMotor.set(speedSupplier.get());
  }
  
  public CANSparkFlex getMotor(){
    return m_FeederMotor;
  }
}
